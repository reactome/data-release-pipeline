import groovy.json.JsonSlurper
// This Jenkinsfile is used by Jenkins to run the Orthopairs step of Reactome's release.
// It requires that the ConfirmReleaseConfigs step has been run successfully before it can be run.
pipeline{
	agent any

	stages{
		// This stage checks that an upstream project, ConfirmReleaseConfig, was run successfully for its last build.
		stage('Check ConfirmReleaseConfig build succeeded'){
			steps{
				script{
					def currentRelease = (pwd() =~ /Releases\/(\d+)\//)[0][1];
					// This queries the Jenkins API to confirm that the most recent build of ConfirmReleaseConfigs was successful.
					def configStatusUrl = httpRequest authentication: 'jenkinsKey', validResponseCodes: '200:404', url: "${env.JENKINS_JOB_URL}/job/$currentRelease/job/ConfirmReleaseConfigs/lastBuild/api/json"
					echo configStatusUrl.getContent();
					if (configStatusUrl.getStatus().toString() == "404") {
						error("ConfirmReleaseConfigs has not yet been run. Please complete a successful build.")
					} else {
						def configStatusJson = new JsonSlurper().parseText(configStatusUrl.getContent())
						echo "HOI"
						echo configStatusJson;
						if (configStatusJson['result'] != "SUCCESS"){
							error("Most recent ConfirmReleaseConfigs build status: " + configStatusJson['result'] + ". Please complete a successful build.")
						}
					}
				}
			}
		}
		// This stage builds the jar file using maven.
		stage('Setup: Build jar file') {
			steps {
				script {
					dir ('orthopairs') {
							sh "mvn clean compile assembly:single"
					}
				}
			}
		}
		// This stage executes the Orthopairs jar file, producing all Orthopairs files used by Orthoinference.
		stage('Main: Generate Orthopairs files') {
			steps {
				script {
					dir ('orthopairs') {
						// The credentials used here are a config file uploaded to Jenkins.
						withCredentials([file(credentialsId: 'Config', variable: 'ConfigFile')]) {
							sh "java -jar target/orthopairs-${env.ORTHOPAIRS_VERSION}-jar-with-dependencies.jar $ConfigFile"
						}
					}
				}
			}
		}
	}
}
