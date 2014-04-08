package com.soteradefense.correlate

import scala.Array.canBuildFrom
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import java.util.Properties
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.clustering.KMeansModel



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
    val K = prop.getProperty("centroids_per_projection").toInt
    val epsilon = prop.getProperty("kmeans_convergence_value").toDouble
    val centroid_dir = prop.getProperty("centroid_dir")
    val projection_dir = prop.getProperty("projection_dir")
    val training_matrix_path = prop.getProperty("training_matrix_path")



    // produce M projection matrices and broadcast to the cluster.
    val projection_matrices = MatrixMath.make_projection_matrices(T, P, M)
    val broadcastProjections = sc.broadcast(projection_matrices)

    
    // read in the vector file, normalize each vector as we read it in
    // reduced_vectors_by_key is a set of strings (the key) tied to an array of M (one for each projection) arrays of doubles (the reduced vector)
    val min_splits = prop.getProperty("min_splits","-1").toInt
    val inputData = if (min_splits > 0) sc.textFile(inputPath,min_splits) else sc.textFile(inputPath)
    val reduced_vectors_by_key = inputData.map(line => {
      val arr = line.trim().split("\t")
      var vector = arr(1).split(",").map(_.toDouble)
      vector = MatrixMath.normalize(vector)
      (arr(0),vector)
    } ).cache()


   // var training_matrix_mapping = reduced_vectors_by_key.map({case (key,vector) => (key,Array.fill[Int](M)(-1))}).cache()

    val kmeans_models = Array.fill[KMeansModel](M)(null)
    val cluster_centers = Array.fill[Array[Array[Double]]](M)(null)
    for (i <- 0 to (M-1)){
      println(s"Projection: $i")
      val matrix = reduced_vectors_by_key.map({ case (key, vector) =>
        (MatrixMath.dotProductVector(vector,broadcastProjections.value(i)))
      }).cache()

      val kmeans = new KMeans()
      kmeans.setK(K)
      kmeans.setMaxIterations(100)
      kmeans.setEpsilon(epsilon)
      kmeans.setInitializationMode("random")
      val model = kmeans.run(matrix)
      //val brModel = sc.broadcast(model)

      kmeans_models(i) = model
      cluster_centers(i) = model.clusterCenters


      matrix.unpersist()
    }
    val broadcastModels = sc.broadcast(kmeans_models)

    val training_matrix_mapping = reduced_vectors_by_key.map( {case (key,vector) =>
      val projections = broadcastProjections.value
      val models = broadcastModels.value
      if (models.length != projections.length)
        throw new IllegalStateException("number of spaces in projections and models does not match, "+projections.length+", and "+models.length)
      val closest_centroids = Array.fill[Int](projections.length)(-1)

      for (i <- 0 to (projections.length -1) ){
        val reduced_vector = MatrixMath.dotProductVector(vector,projections(i))
        closest_centroids(i) = models(i).predict(reduced_vector)
      }
      (key,closest_centroids)
    })

    training_matrix_mapping.saveAsObjectFile(training_matrix_path)
    sc.parallelize(projection_matrices).saveAsObjectFile(projection_dir)
    sc.parallelize(cluster_centers).saveAsObjectFile(centroid_dir)

    println("Training Phase Complete")
  }
  

    
 
}
