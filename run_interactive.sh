#!/bin/bash

export SPARK_MEM=4g
scala -classpath "build/dist/*" com.soteradefense.correlate.CorrelationEngine interactive $1
