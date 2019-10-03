set -e

if [ "$#" -ne 4 ]; then
    echo "This script is used during the SetupReleaseConfig step in Jenkins. It confirms correct configurations that will be used by Jenkins during Reactome's data release."
    echo ""
    echo "Usage: 'bash confirmJenkinsConfigurations.sh configFilepath jenkinsEnvironmentReleaseNumber jenkinsEnvironmentPreviousReleaseNumber userInputReleaseNumber'"
    exit 1
fi

configFile=$1
environmentalReleaseNumber=$2
environmentalPrevReleaseNumber=$3
userInputReleaseNumber=$4

configFileReleaseNumber=

while read line; do
  if [[ $line =~ releaseNumber= ]] ; then
    configFileReleaseNumber=${line#*=}
  fi
done < $configFile

if [[ $userInputReleaseNumber != $environmentalReleaseNumber ]] ; then #|| [[ $userInputReleaseNumber != $configFileReleaseNumber ]]; then
  echo "User input release number ($userInputReleaseNumber) does not match Jenkins environmental release number ($environmentalReleaseNumber)."
  echo "Please update release-specific environmental variables found at 'Jenkins -> Manage Jenkins -> Configure System -> Global Properties'."
  exit 2
fi

if  [[ $userInputReleaseNumber != $configFileReleaseNumber ]]; then
  echo "User input release number ($userInputReleaseNumber) does not match config file release number ($configFileReleaseNumber)."
  echo "Please update config file at 'Jenkins -> Releases -> $userInputReleaseNumber -> Credentials -> master_config.properties(Release-specific credentials)'."
  exit 2
fi

previousReleaseNumber="$(($userInputReleaseNumber-1))"

if [[ $environmentalPrevReleaseNumber != $previousReleaseNumber ]]; then
  echo "Jenkins environmental 'previous' release number ($environmentalPrevReleaseNumber) does not match expected previous release number ($previousReleaseNumber)."
  echo "Please update release-specific environmental variables found at 'Jenkins -> Manage Jenkins -> Configure System -> Global Properties'."
  exit 2
fi

echo "All checked configurations look appropriate."