#!/bin/bash

date
echo "---------------------------------"

hostname
echo "---------------------------------"

echo "java version:"
java -version

#echo "---------------------------------"
#echo "using alternative java version"
#module add java-1.7
#java -version
echo "---------------------------------"

echo "working directory:"
pwd
echo "---------------------------------"

cd ./matsim/santiago-0.8.0-SNAPSHOT-65ad1cfc4ebdd370de8828baff3cddd4c20457e2/

java -Djava.awt.headless=true -Xmx8G -cp santiago-0.8.0-SNAPSHOT.jar playground/santiago/run/SantiagoScenarioRunner ../../santiago/input/config_baseCase.xml
