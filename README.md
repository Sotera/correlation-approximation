Correlation Approximation
=========================

This is a [Spark](http://spark.incubator.apache.org/) implementation of an algorithm to find highly correlated vectors using an approximation algorithm.

We were inspired by google correlate, to learn more about the benefit of fast correlation visit: https://www.google.com/trends/correlate

We particularly encourage you to read the following reading:

  https://www.google.com/trends/correlate/comic
  https://www.google.com/trends/correlate/whitepaper.pdf

Finally take googles implementation for a spin here:
  https://www.google.com/trends/correlate/draw?p=us

This project is meant to bring the power of fast correlation to your data on your cluster.  


Prerequisites
-------------

This project requires the following
  
  * [Spark]  (http://http://spark.incubator.apache.org/)   
  * [Hadoop] (http://hadoop.apache.org/)
  * [Gradle] (http://www.gradle.org/) - to build the analytic
 

Ins and Outs
--------------

Input
  
  We currently take a text file (local or hdfs) for input.  The text must be two tab seberated columns where the first column is a string Key, and the second columns is a vector representing your time series (as a comma sperated list of Doubles)

Output
  
  We have currently have two methods of output

  Bulk - saves a file (local or hdfs) with the correlation values for each pair of keys
  
  Interactive -  command line interface.  Given an input vector returns the top N most highly correlated vector.

In the future we would like to support more input / output formats and redesign our interfaces to be more easily integrated with other work flows.  If you have any ideas or requsests let us know!



Building - A note about hadoop / spark versions
-------------------------------------------------

Our examples are built and tested on Cloudera cdh5.0.0.  Spark and Hadoop are installed and setup on our
cluster using Cloudera Manager.   We recommend using the Cloudera distribution of spark and Hadoop to simplify your
cluster management but any compatible versions of Spark and Hadoop should work.

To build Spark for other Hadoop versions see the Spark documentation.

If you use a different version of spark or hadoop you will have to modify the build.gradle script accordingly.  Depending on your version of spark
you may need to include a dependency on hadoop-client.



Running the a local example
---------------------------

1.  Build the project with gradle
> 'gradle clean dist'

2. Run the training phase to pre-process the input vectors and cache the generated projects and centroids
> './training.sh example/training.properties'

4. Run the bulk mode to correlate every vector against every other vector in the system.
> './run_bulk.sh example/run.properties'

5. Results are stored in the 'output' folder

6. You can also run the interactive example
> './run_interactive /example/run.properites'

7. To remove any cached centroids / projects clean the local directory
> './clean.sh'




Running On a cluster.
----------------------------------

1. Ensure the gradle.build file is setup to use the version of spark running on your cluster (see above)

2.  Build the project
> 'gradle clean dist'  

3. Make a local directory for you cluster configuration
> ' cp -r examples mycluster

4. Move your data to a location on hdfs. If you have small data you can still run on local files, this example assumes you want to use a distributed file system.

5. Edit mycluster/training.properties. 

     set the master uri for your cluster. "master_uri=spark://mymasternode:7077"
     ensure SPARK_HOME is set correctly for your cluster (default set up for cloudera cdh5.0.0-beta-2)
     set the inputPath to your location in hdfs (example inputPath=hdfs://<your name node>/<path to your data> )
     set the output files to point to a location in hdfs
        centroid_dir=hdfs://<namenode>/<path>/generated_centroids
        projection_dir=hdfs://<namenode>/<path>/generated_projections
        training_matrix_path=hdfs://<namenode>/<path>/training_matrix_mapping_v2
     
6. Edit mycluster/run.properties

     set the master uri for your cluster. "master_uri=spark://mymasternode:7077"
     ensure SPARK_HOME is set correctly for your cluster (default set up for cloudera cdh5.0.0-beta-2)
     set the original_data_path to the location of you data in hdfs (example original_data_path=hdfs://<your name node>/<path to your data> )
     set the output path to a location in hdfs
     set centroid_dir, projection_dir, and training_matrix_path to the same as in your training.properties file
     

7. run the training phase on the provided example
> './training.sh mycluster/training.properties'

8. Run the bulk mode to correlate every vector against every other vector in the system.
> './run_bulk.sh mycluster/run.properties'

9. Results are stored in the 'output' folder

10. You can also run the interactive example.  *note: you'll have enter in your hdfs locations instead of the defaul local locations
> './run_interactive /mycluster/run.properites'

11. To remove any cached centroids / projects clean the local directory
> './clean.sh'






Other Information
-----------------

In the training data, and when running interactively, the number of values in the comma separated list must be the same length for every single row.
The data represents a time series and we can only compare time series of the same length.

To clean the directory of any build/training/runtime artifacts, run './clean.sh'

