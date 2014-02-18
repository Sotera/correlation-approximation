#!/bin/bash

rm -rf output
export SPARK_MEM=4g
java -cp "build/dist/*" com.soteradefense.correlate.CorrelationEngine bulk $1
returnCode=$?
exit $returnCode


