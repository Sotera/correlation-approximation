#!/bin/bash

export SPARK_MEM=4g
java -cp "build/dist/*" com.soteradefense.correlate.CorrelationEngine interactive $1
