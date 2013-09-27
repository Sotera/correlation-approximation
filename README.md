Correlation Approximation
=========================

This is a [Spark](http://spark.incubator.apache.org/) implementation of an algorithm to find highly correlated vectors using an approximation algorithm.

Prerequisites
-------------

This project requires the following
  
  * [Scala](www.scala-lang.org)- version 2.9.3
  * [Spark](http://spark.incubator.apache.org/) - version 0.7.3
  * [Hadoop](http://www.cloudera.com/content/cloudera/en/products/cdh.html) - CDH4.2+
  * [Gradle](http://www.gradle.org/) - to build the analytic
  * [sbt](http://www.scala-sbt.org/) - to build Spark

Building Spark
--------------  
Spark must be built against the version of hadoop (cdh) that are using.  While their website does offer binary downloads that are cdh4 compatible, it can still cause trouble.

1. Download spark sources.
2. Extract.
3. Edit '<install_dir>/project/SparkBuild.scala' and modify the follow variables.  This example builds Spark against CDH 4.3.0 __this is important to set corectly__
>  val HADOOP_VERSION = "2.0.0-mr1-cdh4.3.0" </br>
   val HADOOP_MAJOR_VERSION = "2"
4. run 'sbt/sbt clean publish-local'

This will install the spark jars into your local maven repository.

Running Correlation Approximation
----------------------------------

1. Edit build.gradle so make sure the scala/spark/hadoop versions are correct.  The following are entries needed for Scala 2.9.3, Spark v0.7.3 and CDH 4.3.0.  The CDH version (or version of hadoop) should __match what you entered when building spark__.
>  compile('org.spark-project:spark-core_2.9.3:0.7.3')  </br>
   compile('org.apache.hadoop:hadoop-client:2.0.0-mr1-cdh4.3.0')

2. run  'gradle dist' .  
3. run './training.sh examples/training.sh'

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

It will then ask you to enter a test series as comma seperated list of values.  The easiest way to see anything working is top copy part of a that does NOT include the IP address. example: '0,0,1,1,1…'


Other Information
-----------------

In the training data, and when running interactively, the number of values in the comma seperated list must be the same length for every single row.

To clean the directory of any build/training/runtime artifacts, run './clean.sh'

