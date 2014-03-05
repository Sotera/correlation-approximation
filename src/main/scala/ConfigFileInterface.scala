package com.soteradefense.correlate

import java.util.Properties
import java.io.FileInputStream
import java.io.IOException
import org.apache.spark.SparkContext



/**
 * Read in a properties file from the comand line and correlate all vectors using the approximation technique.
 * 
 * usage.  scala -cp ApproximationEngine.jar com.pfi.correlate.ConfigFileInterface test.properties
 * 
 * You must first run TrainingPhase to generate needed files.
 * 
 * If running on a mesos cluster you must specify an hdfs location for training_matrix_path and original_data
 * 
 * example config file
   
   mesos_master_uri=local
   limit=100
   projection_dir=generated_projections
   centroid_dir=generated_centroids
   training_matrix_path=training_matrix_mapping_v2.txt
   original_data_path=time_series_data.txt
   output_dir=output
 */
object ConfigFileInterface {

  
     def run (sc:SparkContext,prop:Properties) = {
     
    
     val engine = Correlator
     engine.sc = sc
     engine.initializeFromProperties(prop)
     
    
     engine.correlateAll( prop.getProperty("min_correlation_score").toDouble, prop.getProperty("max_correlation_score").toDouble, prop.getProperty("output_dir"))
     
     
     
   }
  
  
  
}
