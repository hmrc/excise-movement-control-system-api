#!/bin/sh


./run_dependencies.sh

sbt "run -Drun.mode=Dev -Dhttp.port=10250 $*"