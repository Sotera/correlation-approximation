package com.soteradefense.correlate

import scala.Array.canBuildFrom
import org.apache.spark.SparkContext
import org.apache.commons.math3.stat.clustering.KMeansPlusPlusClusterer
import java.util.Random
import collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import java.io.File
import java.io.IOException
import java.util.Properties
import java.io.FileInputStream

/**
 * Read in a properties file from the comand line and generate training data for the approximation engine.
 * 
 * usage.  scala -cp ApproximationEngine.jar com.pfi.correlate.TrainingPhase training.properties
 * 
 * You must run this program to generate the data you need for a test run.
 * 
 * 
 * example config file
   
   inputPath=time_series_data.txt
   length_of_orignal_vector=192
   length_of_reduced_vector=8
   projections=8
   centroids_per_projection=8
   kmeans_convergence_value=0.001
   centroid_dir=generated_centroids
   projection_dir=generated_projections
   
 */
object TrainingPhase {
	
  
   def run (sc:SparkContext,prop:Properties) = {

    
    val inputPath = prop.getProperty("inputPath")
    val T = prop.getProperty("length_of_orignal_vector").toInt
    val P = prop.getProperty("length_of_reduced_vector").toInt
    val M = prop.getProperty("projections").toInt
    val number_of_clusters = prop.getProperty("centroids_per_projection").toInt
    val epsilon = prop.getProperty("kmeans_convergence_value").toDouble
    val centroid_dir = prop.getProperty("centroid_dir")
    val projection_dir = prop.getProperty("projection_dir")
    val training_matrix_path = prop.getProperty("training_matrix_path")


    // produce M projection matrices and broadcast to the cluster.
    val projection_matrices = MatrixMath.make_projection_matrices(T, P, M)
    val broadcastProjections = sc.broadcast(projection_matrices)

    
    // read in the vector file, normalize each vector as we read it in
    val vectors = sc.textFile(inputPath).map(line => {
      val arr = line.trim().split("\t")
      val vector = arr(1).split(",").map(_.toDouble)
      (arr(0),MatrixMath.normalize(vector))
    } )


    // reduced_vectors_by_key is a set of strings (the key) tied to an array of M (one for each projection) arrays of doubles (the reduced vector)
    val reduced_vectors_by_key = vectors.map( { case(key,vector) =>
      val reduced_vectors = broadcastProjections.value.map(MatrixMath.dotProductVector(vector, _))
      (key,reduced_vectors)
    }).cache
    
      
    
    val unkeyed_vectors = reduced_vectors_by_key.map(_._2)
 
    
    // run kmeans to find centroids for the reduced vectors in each projection
    println("-- entering KMEANS PHASE")
    //val centroids = sc.parallelize(unkeyed_vectors,M).map(MatrixMath.kmeans(_,number_of_clusters,epsilon,100)).collect
    val centroids = unkeyed_vectors.map(MatrixMath.kmeans(_, number_of_clusters, epsilon,100)).collect
    val broadcastCentroids = sc.broadcast(centroids)
    
   
    
    println("-- entering VQ PHASE")
    val training_matrix_mapping = reduced_vectors_by_key.map( {case (key,matrix) => 
      val centroid_mapping = matrix.zip(broadcastCentroids.value).map( {case(vector,centroid_matrix) => 
        MatrixMath.indexofClosesCentroid(centroid_matrix, vector)
      })
      (key,centroid_mapping)
    })
      
     println("-- entering OUTPUT PHASE")
     
      // TRAINING COMPUTATION IS DONE // save what we need as text files.
      
      
     //makeDir(centroid_dir)
     //makeDir(projection_dir)
      
     // var printableArray = training_matrix_mapping.map({case (key,vector) => key+"\t"+vector.map(_.toString).reduceLeft(_+","+_)})
     // printToFile( training_matrix_path, printableArray )
      training_matrix_mapping.saveAsObjectFile(training_matrix_path)
      
     // important, we only want one slice so our ordering is preserved.
     // this might not work / scale with very large inputs
     sc.parallelize(centroids, 1).saveAsObjectFile(centroid_dir)
     sc.parallelize(projection_matrices, 1).saveAsObjectFile(projection_dir)
      
     /*
      // for projections and centroids we create one file for each M
      for (m<-0 until M){
        printableArray = centroids(m).map(_.map(_.toString()).reduceLeft(_+","+_))
        printToFile(centroid_dir+"/"+m.toString,printableArray)
        
        printableArray = projection_matrices(m).map(_.map(_.toString()).reduceLeft(_+","+_))
        printToFile(projection_dir+"/part"+m.toString,printableArray)
      }*/
      
      println("Training Phase Complete")
     
     
  }
  
  /*
  // print an array of strings to a local file
  def printToFile(filename:String,values:Array[String]) = {
    println("Writing output to file: "+filename)
    val out_file = new java.io.FileOutputStream(filename)
    val out_stream = new java.io.PrintStream(out_file)
    values.foreach(out_stream.println(_))
    out_file.close()
    out_stream.close()
  }
  
  
  // make a directory in the local file system
  def makeDir(dirname:String) = {
    val dir = new File(dirname)
    if (!dir.exists()) {
      if (dir.mkdir()) println("created new dir: "+dirname)
      else println("failed to create new dir: "+dirname)    
    }
  }*/
    
 
}
