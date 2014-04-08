package com.soteradefense.correlate

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation
import scala.collection.mutable.ArrayBuffer
import java.util.Random


/**
 * Various functions to perform vector and array operations
 */
object MatrixMath {

  
	def pearsonsCorrelate (v1 :Array[Double], v2 : Array[Double]) : (Double) = {
	  new PearsonsCorrelation().correlation(v1,v2)
	}
	
  
	def approximateDistance (distanceHash: Array[Array[Double]], vector: Array[Int]): (Double) = {
      var total = 0.0
      for (i <- 0 until vector.length ){
        total = total + scala.math.pow(distanceHash(i)(vector(i)),2)
      }
      scala.math.sqrt(total)
  }
  
  
	def dotProduct (matrix1: Array[Array[Double]], matrix2: Array[Array[Double]]) : (Array[Array[Double]]) = {
		// check for valid input
		if (matrix1(0).length != matrix2.length){
			throw new IllegalArgumentException("Invalid matrix dimensions  matrix1 columns: "+matrix1(0).length+" matrix2 rows: "+matrix2.length)

		}

		var outputArray = new Array[Array[Double]](matrix1.length)
				for (i <-0 until outputArray.length ){
					outputArray(i) = new Array[Double](matrix2(0).length)
				}

		var result =0.0;
		for (i<-0 until matrix1.length){
			// for each row of the first matrix
			for (j<-0 until matrix2(0).length){
				// for each colum of the second matrix
				result = 0;
				for (k<-0 until matrix1(0).length ){
					result = result + matrix1(i)(k)*matrix2(k)(j)

				}
				outputArray(i)(j) = result
			}
		}
		return outputArray
	}

	def dotProductVector(vector:Array[Double],matrix:Array[Array[Double]]) :(Array[Double]) = {
	  dotProduct(Array(vector),matrix)(0)
	}

	
	def dotProductKeyedMatrix (matrix1: Array[(String,Array[Double])], matrix2: Array[Array[Double]]) : (Array[(String,Array[Double])]) = {
		
	   if (matrix1(0)._2.length != matrix2.length){
			throw new IllegalArgumentException("Invalid matrix dimensions  matrix1 columns: "+matrix1(0)._2.length+" matrix2 rows: "+matrix2.length)
		}

		var outputArray = new Array[(String,Array[Double])](matrix1.length)
		for (i <-0 until outputArray.length ){
		  outputArray(i) = (matrix1(i)._1,new Array[Double](matrix2(0).length))
		}

		var result =0.0;
		for (i<-0 until matrix1.length){
			// for each row of the first matrix
			for (j<-0 until matrix2(0).length){
				// for each colum of the second matrix
				result = 0;
				for (k<-0 until matrix1(0)._2.length ){
					result = result + matrix1(i)._2(k)*matrix2(k)(j)
				}
				outputArray(i)._2(j) = result
			}
		}
		return outputArray
	}
	
	
	def normalize (vector : Array[Double]) : (Array[Double]) = {
	  val sum_of_squres = vector.map(math.pow(_,2)).reduceLeft(_+_)
	  vector.map(_/math.sqrt(sum_of_squres))
	}



	def vectorDistance (v1: Array[Double], v2: Array[Double]) : (Double) = {

		if (v1.length != v2.length){
			throw new IllegalArgumentException("Vector lengths must match")
		}

		var result = 0.0
		for (i<-0 until v1.length){
		  result = result + scala.math.pow(v1(i)-v2(i),2)
		}
		scala.math.sqrt(result)

	}
	
	
	// create projection matrices during training phase
	def make_projection_matrices(T:Int,p:Int,M:Int): (Array[Array[Array[Double]]]) = {
    // param T: the length of the time series
    // param p: the dimension of each projection
    // param M: the number of projection matrices
    // returns a list of M (Txp) random matrices
    
		val matrix_list = new Array[Array[Array[Double]]](M)
		for (i <- 0 until M){
		  matrix_list(i)  = getRandomNormalMatrix(T,p)
		}
		matrix_list
		
	}
	
	
	// return a rows X cols matrix where each row is normalized and is a random
	// selection from the normal distribution
	def getRandomNormalMatrix(rows:Int,cols:Int): (Array[Array[Double]]) = {
	  val rand = new Random()
	  Array.fill(rows) {getRandomNormalVector(cols,rand)}
	}
	
	def getRandomNormalVector(size:Int,rand:Random): (Array[Double]) = {
	  //val vector = new Array[Double](size)
	  val vector = Array.fill(size){rand.nextGaussian}
	  normalize(vector)
	  
	}
	
	def getAverageVector(matrix:Array[Array[Double]]) : (Array[Double]) = {
	  // calculate the average vector
      val rows = matrix.length
      val cols = matrix(0).length
      val avg = new Array[Double](cols)
      for (i<-0 until cols){
        var row_sum = 0.0
        for (j<-0 until rows){
          row_sum += matrix(j)(i)
        }
        avg(i) = row_sum / rows
      }
      avg
	}
    
	
	/**
   * vectors- a square matrix of dobules
   * k - number of clusters to generate
   */
  def kmeans(vectors:Array[Array[Double]],k:Int,epsilon:Double,maxIt:Int) : (Array[Array[Double]])= {
     
    // initialize some random centroids
    var centroids = MatrixMath.getRandomNormalMatrix(k, vectors(0).length)
    
    var movement = Array(epsilon+1)
    var iterations = 0
    while (movement.exists(_ > epsilon) && iterations < maxIt){
      iterations += 1
      // assign each vector to a centroid
      val clusters = vectors.groupBy(closestCentroid(centroids,_))
    
      // Recalculate centroids as the average of the points in their cluster
      // (or leave them alone if they don't have any points in their cluster)
      val newCentroids = centroids.map(oldCentroid => {
        clusters.get(oldCentroid) match {
          case Some(matrix) => MatrixMath.getAverageVector(matrix)
          case None => oldCentroid
        }})
      
    
      // Calculate the centroid movement for the stopping condition
      movement = (centroids zip newCentroids).map({ case (a, b) => MatrixMath.vectorDistance(a, b)})

      /*
      System.err.println("Centroids changed by\n" +
            "\t   " + movement.map(d => "%3f".format(d)).mkString("(", ", ", ")") + "\n" +
            "\tto " + newCentroids.mkString("(", ", ", ")"))
            */
      centroids = newCentroids
    }
    centroids
      
  }
  
  
  
  // used by kmeans
  private def closestCentroid(centroids: Array[Array[Double]], vector:Array[Double]) = {
    centroids.reduceLeft((a, b) => if ( MatrixMath.vectorDistance(vector,a) < MatrixMath.vectorDistance(vector, b) ) a else b )
  }
  
  def indexofClosesCentroid(centroids: Array[Array[Double]], vector:Array[Double]) : (Int) = {
    val distance = centroids.map(MatrixMath.vectorDistance(vector,_))
    var max = 0
    for (i<-0 until distance.length){
      if (distance(i) > distance(max) ) max = i
    }
    max
  }



}