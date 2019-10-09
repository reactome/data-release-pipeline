import groovy.json.JsonSlurper
// This Jenkinsfile is used by Jenkins to run the DownloadDirectory step of Reactome's release.
// It requires that the AddLinks-Insertion step has been run successfully before it can be run.
pipeline {
	agent any

	stages {
		// This stage checks that an upstream project, AddLinks-Insertion, was run successfully for its last build.
		stage('Check AddLinks-Insertion build succeeded'){
			steps{
				script{
					def addLinksInsertionStatusUrl = httpRequest authentication: 'jenkinsKey', url: "${env.JENKINS_JOB_URL}/job/${env.RELEASE_NUMBER}/job/AddLinks-Insertion/lastBuild/api/json"
					def addLinksInsertionJson = new JsonSlurper().parseText(addLinksInsertionUrl.getContent())
					if(addLinksInsertionJson['result'] != "SUCCESS"){
						error("Most recent AddLinks-Insertion build status: " + addLinksInsertionJson['result'] + ". Please complete a successful build.")
					}
				}
			}
		}
		// This stage clones, builds, and install the Pathway-Exchange dependency needed for DownloadDirectory.
		stage('Setup: Install Pathway Exchange artifact'){
			steps{
				script{
					dir('download-directory'){
						sh "./build_pathway_exchange.sh"
					}
				}
			}
		}
		// This stage builds an archive containing the download directory jar and its dependencies.
		// It also unpacks that archive to be used in the following stage.
		stage('Setup: Build DownloadDirectory archive'){
			steps{
				script{
					dir('download-directory'){
						sh "mvn clean package -DskipTests"
						sh "rm -rf downloadDirectory"
						sh "unzip -o target/downloadDirectory-distr.zip"
					}
				}
			}
		}
		// This stage executes the DownloadDirectory code. It generates various files that are downloadable from the reactome website.
		// The files that are produced are configurable. See the 'Running specific modules of Download Directory' section in the README.
		stage('Main: Run DownloadDirectory'){
			steps{
				script{
					dir('download-directory'){
						withCredentials([file(credentialsId: 'Config', variable: 'ConfigFile')]){
							withCredentials([file(credentialsId: 'stepsToRun', variable: 'StepsToRun')]){
								sh "java -Xmx${env.JAVA_MEM_MAX}m -javaagent:downloadDirectory/lib/spring-instrument-4.2.4.RELEASE.jar -jar downloadDirectory/downloadDirectory.jar $ConfigFile $StepsToRun"
							}
						}
					}
				}
			}
		}
		// This stage archives all logs and other outputs produced by DownloadDirectory.
		stage('Post: Archive logs and validation files'){
			steps{
				script{
					dir('download-directory'){
						sh "mkdir -p archive/${env.RELEASE_NUMBER}/logs"
						sh "mv --backup=numbered biopax*validator.zip archive/${env.RELEASE_NUMBER}/"
						sh "gzip logs/*"
						sh "mv logs/* archive/${env.RELEASE_NUMBER}/logs/"
					}
				}
			}
		}
	}
}
