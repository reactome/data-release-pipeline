#!/bin/bash
set -e

DIR=$(dirname "$(readlink -f "$0")") # Directory of the script -- allows the script to invoked from anywhere
cd $DIR

configFile=
environmentalReleaseNumber=
environmentalPrevReleaseNumber=
userInputReleaseNumber=

## Parse command-line arguments.
while (( "$#" )); do
	case "$1" in
		-c|--config)
			configFile=$2;
			shift 2;
			;;
		-e|--env-release-current)
			environmentalReleaseNumber=$2;
			shift 2;
			;;
		-p|--env-release-previous)
			environmentalPrevReleaseNumber=$2;
			shift 2;
			;;
		-u|--user-release)
			userInputReleaseNumber=$2;
			shift 2;
			;;
		-*|--*=)
			echo "Error: Unsupported flag $1"
			exit 1
	esac
done

## If missing arguments, explain usage.
if [ -z "$configFile" ] || [ -z "$environmentalReleaseNumber" ] || [ -z "$environmentalPrevReleaseNumber" ] || [ -z "$userInputReleaseNumber" ]
then
	echo "Confirm Jenkins configurations.";
	echo "Compares 'release number' values for config file, Jenkins environment and from user input."; 
	echo "Usage: bash confirmJenkinsConfigurations.sh --config configFilepath --env-release-current environmentCurrentReleaseNumber --env-release-previous environmentPreviousReleaseNumber --user-release userInputReleaseNumber ";
	exit 1
fi

configFileReleaseNumber=

## Iterate through lines of config file, which should be in .properties format, looking for 'releaseNumber' value.
while read line; do
  if [[ $line =~ releaseNumber= ]] ; then
    configFileReleaseNumber=${line#*=}
  fi
done < $configFile

## Compare user input release number value with jenkins environment's release number value.
if [[ $userInputReleaseNumber != $environmentalReleaseNumber ]] ; then 
  echo "User input release number ($userInputReleaseNumber) does not match Jenkins environmental release number ($environmentalReleaseNumber)."
  echo "Please update release-specific environmental variables found at 'Jenkins -> Manage Jenkins -> Configure System -> Global Properties'."
  exit 1
fi

## Compare user input release number value with config file's release number value.
if  [[ $userInputReleaseNumber != $configFileReleaseNumber ]]; then
  echo "User input release number ($userInputReleaseNumber) does not match config file release number ($configFileReleaseNumber)."
  echo "Please update config file at 'Jenkins -> Releases -> $userInputReleaseNumber -> Credentials -> master_config.properties(Release-specific credentials)'."
  exit 1
fi

## Compare previous release number, created by subtracting from user input release number, with jenkins environment's previous release number value.
previousReleaseNumber="$(($userInputReleaseNumber-1))"

if [[ $previousReleaseNumber != $environmentalPrevReleaseNumber ]]; then
  echo "Jenkins environmental 'previous' release number ($environmentalPrevReleaseNumber) does not match expected previous release number ($previousReleaseNumber)."
  echo "Please update release-specific environmental variables found at 'Jenkins -> Manage Jenkins -> Configure System -> Global Properties'."
  exit 1
fi

echo "All checked configurations look appropriate."
