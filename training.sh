#!/bin/bash
# verify data is loaded, and exit if not.
basedir=$PWD

export SPARK_MEM=4g

rm -rf generated_centroids
rm -rf generated_projections
java -cp "build/dist/*" com.soteradefense.correlate.CorrelationEngine train $1
