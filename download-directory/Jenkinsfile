import groovy.json.JsonSlurper

pipeline {
    	agent any

	stages {
		stage('Check upstream builds succeeded'){
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
	    
		stage('Setup: Install Pathway Exchange artifact'){
			steps{
				script{
					dir('download-directory'){
						sh './build_pathway_exchange.sh'
					}
				}
			}
		}
        	stage('Setup: Build DownloadDirectory archive'){
            		steps{
				script{
                    			dir('download-directory'){
						sh 'mvn clean package -DskipTests'
			    			sh 'rm -rf downloadDirectory'
						sh 'unzip -o target/downloadDirectory-distr.zip'
                			}
          			}
            		}
        	}
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
