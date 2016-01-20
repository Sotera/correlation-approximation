/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
 
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
