#!/bin/bash
DIR=$(dirname "$(readlink -f "$0")") # Directory of the script -- allows the script to invoked from anywhere
cd $DIR

## Update repo
git pull
## Create new jar file with orthoinference code
mvn clean compile assembly:single

## set args - all required
configPath="src/main/resources/config.properties"	# Path to main configuration file
refSpecies="orig"	# Reference species (4-char abbv)
projSpecies=(prj1 prj2 prj3)	# Projected species (4-char abbv); may contain multiple species, space-delimited


## Run orthoinference for each species
for species in "${projSpecies[@]}"
do
	cmd="java -jar target/orthoinference-0.0.1-SNAPSHOT-jar-with-dependencies.jar $configPath $refSpecies $species > orthoinference_$species.out;"
	echo $cmd
	eval $cmd
done
echo "Orthoinference complete"