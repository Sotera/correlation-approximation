# Why Correlation Approximation?

The correlation approximation engine is a spark-based implementation of the more well known Google Correlate.  

When analyzing a new time series you may want to compare it against a bank of existing time series data to discover possible relationships in the data.  

![Time Series of IP Address Counts](https://raw.github.com/Sotera/correlation-approximation/master/docs/images/corrapprox_timeseries.PNG)

Direct comparison of your time series against each series in the bank may work for small or moderate size datasets, but with large data sets and long vectors the operation could take longer than a user is willing to wait.   By using a scalable approximation technique you can answer these types of correlation queries on huge sets of data very quickly.

For more information on the origins of correlation approximation see Google correlate:

  [Google Correlate](https://www.google.com/trends/correlate)
  
  [Google Correlate Comic](https://www.google.com/trends/correlate/comic)

  [Google Correlate White Paper](https://www.google.com/trends/correlate/whitepaper.pdf)


# Implementation notes

This implementation is currently much simpler than Google Correlate.  We've started with a simple system that can read local or hdfs files and can provide correlation results in local or hdfs files.   We've also included a simple interactive command line interface.


Prerequisites
-------------

This project requires the following
  
  * [Spark]  (http://http://spark.incubator.apache.org/)   
  * [Hadoop] (http://hadoop.apache.org/)
  * [Gradle] (http://www.gradle.org/) - to build the analytic
 

Process
-------

The correlation approximation system runs a two step process.

1. Training Phase - loading your data.

   Large set of time series data (or any numeric vectors) is read in to the system
   and reduced to several smaller projections of the data.  K-means centroids are found for each projection.
   The projects, reduced vectors, and centroids are cached for use in the next phase.
   For a complete description of the algorithms see [The Google White Paper](https://www.google.com/trends/correlate/whitepaper.pdf)
   The number of projections as well as the dimensions of each projection and number of centroids to     calculate is easily configurable. 

2. Test Phase - testing a new vector against the cached data

    In this phase the system loads the reduced vectors, projection data, and centroids from the training
    phase and uses them to quickly find the top N (default to 100) most highly correlated vectors from your
    data set.

## Training Phase Input
  
We currently take a text file (local or hdfs) for input.  The text must be two tab seberated columns where the first column is a string Key, and the second columns is a vector representing your time series (as a comma sperated list of Doubles)  For an example see [test_data.tsv](https://github.com/Sotera/correlation-approximation/blob/master/example/test_data.tsv).  All vectors must be of equal length.

## Training Phase Output

Output data from the training phase is written as object files (not human readable) to local files or to hdfs.

## Test Phase Input and Output

### Bulk Mode 

Bulk mode is a method to test the system performance and accuracy by correlating all the vectors in the system against each other.  No additional input is required, the system uses the original data from the training phase.  Output is written to a local or hdfs file.

### Interactive Mode

Interactive Mode is a simple command line program.  You'll specify some configuration information on the command line and you'll then be able to enter time series data as a comma separated list of doubles.  For each time series you enter you'll be returned the most highly correlated vectors from the training set.


### Batch Mode (coming soon)

A command line tool for correlating all vectors in a given input file (local or hdfs) and supplying the results to an output file (local or hdfs)


# Simple Example

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
> gradle clean dist

2. Run the training phase to pre-process the input vectors and cache the generated projects and centroids
> './training.sh example/training.properties'

4. Run the bulk mode to correlate every vector against every other vector in the system.
> ./run_bulk.sh example/run.properties

5. Results are stored in the 'output' folder

6. You can also run the interactive example
> ./run_interactive example/run.properites

7. To remove any cached centroids / projects clean the local directory
> ./clean.sh




Running On a cluster.
----------------------------------

1. Ensure the gradle.build file is setup to use the version of spark running on your cluster (see above)

2.  Build the project
> gradle clean dist  

3. Make a local directory for you cluster configuration
> cp -r examples mycluster

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
> ./training.sh mycluster/training.properties

8. Run the bulk mode to correlate every vector against every other vector in the system.
> ./run_bulk.sh mycluster/run.properties

9. Results are stored in the 'output' folder

10. You can also run the interactive example.  *note: you'll have to enter your hdfs locations instead of the default local locations
> './run_interactive /mycluster/run.properites'

