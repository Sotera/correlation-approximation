Correlation Approximation
=========================

This is a [Spark](http://spark.incubator.apache.org/) implementation of an algorithm to find highly correlated vectors using an approximation algorithm.

Prerequisites
-------------

This project requires the following
  
  * [Spark]  (http://http://spark.incubator.apache.org/)   
  * [Hadoop] (http://hadoop.apache.org/)
  * [Gradle] (http://www.gradle.org/) - to build the analytic
 

Building - A note about hadoop / spark versions
_______________________________________________

Our examples are built and tested on Cloudera cdh5.0.0.  Spark and Hadoop are installed and setup on our
cluster using Cloudera Manager.   We recommend using the Cloudera distribution of spark and Hadoop to simplify your
cluster management but any compatible versions of Spark and Hadoop should work.

To build Spark for other Hadoop versions see the Spark documentation.

If you use a different version of spark or hadoop you will have to modify the build.gradle script accordingly.  Depending on your version of spark
you may need to include a dependency on hadoop-client.



Running the a local example
_______________________________________

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
     
6. Edit mycluster/run.properties

     set the master uri for your cluster. "master_uri=spark://mymasternode:7077"
     ensure SPARK_HOME is set correctly for your cluster (default set up for cloudera cdh5.0.0-beta-2)
     set the original_data_path to the location of you data in hdfs (example original_data_path=hdfs://<your name node>/<path to your data> )
     set the output path to a location in hdfs
     

7. run the training phase on the provided example
> './training.sh mycluster/training.properties'

8. Run the bulk mode to correlate every vector against every other vector in the system.
> './run_bulk.sh mycluster/run.properties'

9. Results are stored in the 'output' folder

10. You can also run the interactive example
> './run_interactive /mycluster/run.properites'

11. To remove any cached centroids / projects clean the local directory
> './clean.sh'


Next you can run the analytic interactively or in bulkmode.  



Bulk Mode
---------
To run in bulk, and have the output written to disk, execute
> './run_bulk.sh example/run.properties'  

The output is in output/part-00000 file.
The format is tab delimited
> ID1 ID2 Correlation-coeffecient

Interactive Mode
----------------
To run it interactively through the shell, execute
> './run_interactive.sh'

This will ask you a bunch of questions that you can find the answers to in example/run.properties'.  The defaults suggestions will work.

It will then ask you to enter a test series as comma separated list of values.  The easiest way to see anything working is top copy part of a that does NOT include the IP address. example: '0,0,1,1,1���'


Other Information
-----------------

In the training data, and when running interactively, the number of values in the comma separated list must be the same length for every single row.

To clean the directory of any build/training/runtime artifacts, run './clean.sh'

