package com.soteradefense.correlate

import java.io.IOException
import java.util.Properties
import java.io.FileInputStream
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._



object CorrelationEngine{
     
     def main (args: Array[String]) = {
       
       if (args.length != 2){
         println("Specify the action you want to take: (train,interactive,bulk) and a config file.")
         System.exit(1)
       }
       val action = args(0)
       val config_file_path = args(1)

       val config = new Properties()
       try{
         config.load(new FileInputStream(config_file_path))
        } catch {
          case e: IOException => { 
            println("Error reading config file: "+config_file_path); 
            System.exit(1)
          }
        }

      val masterUri = config.getProperty("master_uri","local")
      val spark_home = config.getProperty("SPARK_HOME","")
      val deploymentCodePaths = config.getProperty("deployment_path","").split(":")

      val sc : SparkContext = 
         if (masterUri == "local")
           new SparkContext("local","ApproximationEngine-"+action)
             else
    	       new SparkContext(masterUri,"ApproximationEngine-"+action,spark_home,deploymentCodePaths)


       val remaining_args = args.drop(1)
       action match {
         case "train" => TrainingPhase.run(sc,config)
         case "interactive" => CommandLineCorrelate.run(sc,config)
         case "bulk" => ConfigFileInterface.run(sc,config)
         case "bruteforce" => BruteForce.run(sc,config)
         case _ => {
             println("Specify the action you want to take: (train,interactive,bulk)")
             System.exit(1)
         }
       }

     sc.stop()
     }
  

 }



