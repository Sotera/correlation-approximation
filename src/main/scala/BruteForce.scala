package com.soteradefense.correlate

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import java.util.Properties
import org.apache.spark.rdd.RDD

/**
 * Brute force correlation using a cartesian join to
 * calculate the correlation for all pairs of vectors
 *
 * Intended to validate results, not for large distributed computation
 */
object BruteForce {

  def run (sc:SparkContext,prop:Properties) = {

    val inputPath = prop.getProperty("inputPath")
    val outputPath = prop.getProperty("outputPath")
    val min_splits = prop.getProperty("min_splits", "-1").toInt
    val inputData = if (min_splits > 0) sc.textFile(inputPath, min_splits) else sc.textFile(inputPath)

    val reduced_vectors_by_key = inputData.map(line => {
      val arr = line.trim().split("\t")
      var vector = arr(1).split(",").map(_.toDouble)
      vector = MatrixMath.normalize(vector)
      (arr(0), vector)
    }).cache()

    val fulljoin = reduced_vectors_by_key.cartesian(reduced_vectors_by_key).map({
      case ((key1, vector1), (key2, vector2)) =>
        (MatrixMath.pearsonsCorrelate(vector1,vector2),(key1,key2))
    }).sortByKey(false)
    .map({case (corr,(key1,key2)) => (key1,(key2,corr))})
    .sortByKey()
    .map( {case (key1,(key2,corr)) => s"$key1\t$key2\t$corr" })
    .saveAsTextFile(outputPath)



  }

}
