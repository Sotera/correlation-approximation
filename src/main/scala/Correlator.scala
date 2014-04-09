package com.soteradefense.correlate

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.math
import scala.Console
import java.io.IOException
import java.io.File
import scala.io.Source
import scala.collection.mutable.ArrayBuffer
import scala.runtime.ScalaRunTime._
import org.apache.spark.rdd.RDD
import java.util.Properties
import org.apache.spark.broadcast.Broadcast

object Correlator {
	
    var sc:SparkContext = null
    var limit:Int = 100
    var trainingMatrixRDD:RDD[(String,Array[Int])] = null
    var originalVectorsRDD:RDD[(String,Array[Double])] = null
    var projection_matricies:Broadcast[Array[Array[Array[Double]]]]  = null
    var centroid_matricies:Broadcast[Array[Array[Array[Double]]]] = null
    private var initialized = false
   
    
    /**
     * Read the training data and original data into memory, must be done before any correlation
     * projection_dir: directory containing the M projection matricies
     * centroid_dir: directory containing centroids for each projeciton
     * training_matrix_path: file mapping each vector to its associated centroid in each projection
     * original_data_path: path to the file containing the original vectors. used to correlate top 100 matches
     */
    def initialize(projection_dir:String, centroid_dir:String, training_matrix_path:String,original_data_path:String,config:Properties) = {
      
      
      trainingMatrixRDD = sc.objectFile[(String,Array[Int])](training_matrix_path)
      
      /*
      trainingMatrixRDD = sc.textFile(training_matrix_path).map(row =>{
        val rowArray = row.split("\t")
        val vector = rowArray(1).split(",").map(_.toInt)
        (rowArray(0),vector)
      })*/
      val min_splits = config.getProperty("min_splits","-1").toInt
      val inputData = if (min_splits > 0) sc.textFile(original_data_path,min_splits) else sc.textFile(original_data_path)
      originalVectorsRDD = inputData.map( line =>
      {
    	  var arr = line.trim().split("\t")
	      (arr(0),MatrixMath.normalize(arr(1).split(",").map(_.toDouble)))
	    }).cache() // cache because we will hit this dataset several times
	  
	  
	    projection_matricies = sc.broadcast(sc.objectFile[Array[Array[Double]]](projection_dir).collect())
	    centroid_matricies = sc.broadcast(sc.objectFile[Array[Array[Double]]](centroid_dir).collect())
	    initialized = true
    }
    
    
    
    def initializeFromProperties(prop:Properties)  = {
     limit = prop.getProperty("limit").toInt;
     val projection_dir = prop.getProperty("projection_dir");
     val centroid_dir = prop.getProperty("centroid_dir")
     val training_matrix_path = prop.getProperty("training_matrix_path")
     val original_data_path = prop.getProperty("original_data_path")
     initialize(projection_dir,centroid_dir,training_matrix_path,original_data_path,prop)
    }
    
    
    // After initialize call this method to correlate a vector (as a comma seperated string) against 
    // the training data.
    def correlate( inputStr:String) : Array[(Double, (Double, String))]= {
        if (!initialized){
          throw new IllegalStateException("Correlator not initialized!")
        }
      
	      val test_series = MatrixMath.normalize(inputStr.split(",").map(_.toDouble))
	      val distance_hash = createDistanceHash(test_series,projection_matricies.value,centroid_matricies.value)
        val dhBroadcast = sc.broadcast(distance_hash)
	      // this is a little clumsy.. the take operation changes us from an RDD to a local array, then we go back to an RDD
	      // to do the correlation with the actual vectors.  But we need to do the take operation as it can significantly reduce
	      // the amount of data we are dealing with

	      val results = trainingMatrixRDD.map( { case(training_key,training_vector) =>
	        val distance = MatrixMath.approximateDistance( dhBroadcast.value,training_vector)
	        (distance,training_key)
	      } )
	      .sortByKey().take(limit)
	    
	    
	    
	      val resultsRDD = sc.parallelize(results).map( {case (distance,key) => (key,distance) }) // change the ordering back to key,approximate distance.
	      resultsRDD.join(originalVectorsRDD).map( {case(key,(distance,vector)) =>
            val corr = MatrixMath.pearsonsCorrelate(test_series,vector)
            (corr,(distance,key))
	      }).filter(!_._1.isNaN()).sortByKey(false).collect()
    }
   
    
    
  
    
    // Correlate all vector files against each other.
    def correlateAll( minthreshold:Double,maxthreshold:Double,output_path:String) = {
       if (!initialized){
          throw new IllegalStateException("Correlator not initialized!")
        }
       
       // this seems silly, but we have to get the global variable into the local scope
       // so the anonymous function will pick it up, otherwise when the anonymous function
       // executes remotely it will pick up its own copy of the global, which will be null.
       val projections = projection_matricies
       val centroids = centroid_matricies
       val vectors = sc.broadcast(trainingMatrixRDD.collect())
	
       // for every vector calculate the approximate distance to ever other vector
       // for the top 100 closest emit (training_key,(test_key,test_vector)) pairs
	   originalVectorsRDD.flatMap( {case (test_key,test_vector) =>
	     
	     val distanceHash = createDistanceHash(test_vector,projections.value,centroids.value)
	     vectors.value.filter( _._1 > test_key).map({case (training_key,training_index_vector) =>
	        val distance = MatrixMath.approximateDistance( distanceHash,training_index_vector )
	        (training_key,distance)
	     }).sortBy(_._2).reverse.take(limit).map({case (training_key,distance) => (training_key,(test_key,test_vector)) })
	     
	   })
	   

	    // do a join to get the test_vector and training vector for each pair, then calculate actual correlation
	    // note that the join against originalVectorsRDD should be fast because it is cached 
	    .join(originalVectorsRDD).map( {case ( (training_key,((test_key,test_vector),training_vector) ))  =>
	      val result = MatrixMath.pearsonsCorrelate(test_vector, training_vector)
	      (test_key+"\t"+training_key,result)
	    })
	    .filter( {case(keys,result) => !result.isNaN() && (result < minthreshold || result > maxthreshold) })
	    .sortByKey()
	    .map({ case(keys,result) => keys+"\t"+result.toString()}).saveAsTextFile(output_path)  
	     
	 
    }
    
    
    private def createDistanceHash(vector:Array[Double],projections:Array[Array[Array[Double]]],centroids:Array[Array[Array[Double]]]): (Array[Array[Double]]) = {
     
      val M = projections.length
      // generate a reduced vector for each projection
	    var reduced_test_vector_matrix = new Array[Array[Double]](M)
	    for (i<-0 until M ){
		    reduced_test_vector_matrix(i) = MatrixMath.dotProduct(Array(vector),projections(i))(0)
	    }
      
      var distance_hash = new Array[Array[Double]](M)
	    for (i<-0 until M){
		    var row = new Array[Double](centroids(i).length)
	      for(j<-0 until centroids(i).length){
	    	  row(j) = MatrixMath.vectorDistance(reduced_test_vector_matrix(i),centroids(i)(j))
	      }
	      distance_hash(i) = row
	    }
      distance_hash
    }
    
    
    
    /**
    // read in the projection matricies
    private def getProjectionMatricies(projection_dir:String) : (Array[Array[Array[Double]]])= {
      // generate a reduced vector for each projection
	    var files : (Array[File]) = new File(projection_dir).listFiles
	    val m = files.length  // number of projections
	    val projection_matricies = new Array[Array[Array[Double]]](m);
	    for (i<-0 until m ){  
	      val buffer = new ArrayBuffer[Array[Double]]
	      for (line<- Source.fromFile(new File(projection_dir+"/part"+i).getPath).getLines){
	    	  //buffer += line.trim().split("\t")(1).split(",").map(_.toDouble)
	        // removed unused inex from file
	         buffer += line.trim().split(",").map(_.toDouble)
	      }
	      projection_matricies(i) = buffer.toArray
	    }
	    projection_matricies
    }*/
    
    /**
    // read in the centroid matrices
    private def getCentroidMatricies(centroid_dir:String,M:Int) : (Array[Array[Array[Double]]])= {
      val centroid_matricies = new Array[Array[Array[Double]]](M)
	  for (i<-0 until M){
		  val buffer = new ArrayBuffer[Array[Double]]
		  for (line<- Source.fromFile(new File(centroid_dir+"/"+i)).getLines){
    	  	buffer += line.trim().split(",").map(_.toDouble)
		  }
		  centroid_matricies(i) = buffer.toArray
	    }
      centroid_matricies
    }*/
    
   
    
    
    
    
}
