#!/bin/bash
if [ -z $1 ]; then
  echo "first parameter is empty. it needs to be an ideafix version number"
  exit 0
fi
cd examples
gradle clean
cd ..
mkdir ideafixSdk-$1
cd ideafixSdk-$1
mvn dependency:copy -Dartifact=com.fixisoft:ideafix:$1  -DoutputDirectory=.
mvn dependency:copy -Dartifact=com.fixisoft:ideafix:$1:pom   -DoutputDirectory=.
cp ../install.sh .
sed -i -e "s/#version/$1/g" ./install.sh
cp -r ../examples .
cp -r ../benchmarks .
cd ..
zip -r ideafixSdk-$1.zip ./ideafixSdk-$1/*
