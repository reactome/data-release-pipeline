import groovy.json.JsonSlurper

pipeline{
	agent any
	
	stages{
		stage('Check if upstream builds succeeded'){
			steps{
				script{
					def orthopairsStatusUrl = httpRequest authentication: 'jenkinsKey', url: "${env.JENKINS_JOB_URL}/job/${env.RELEASE_NUMBER}/job/Orthopairs/lastBuild/api/json"
					def orthopairsStatusJson = new JsonSlurper().parseText(orthopairsStatusUrl.getContent())
					if(orthopairsStatusJson['result'] != "SUCCESS"){
						error("Most recent Orthopairs build status: " + orthopairsStatusJson['result'])
					}
			    }
				script{
					def updateStIdsStatusUrl = httpRequest authentication: 'jenkinsKey', url: "${env.JENKINS_JOB_URL}/job/${env.RELEASE_NUMBER}/job/UpdateStableIdentifiers/lastBuild/api/json"
					def updateStIdsStatusJson = new JsonSlurper().parseText(updateStIdsStatusUrl.getContent())
					if(updateStIdsStatusJson['result'] != "SUCCESS"){
						error("Most recent UpdateStableIdentifiers build status: " + updateStIdsStatusJson['result'])
					}
				}
	    	}
		}
		stage('Setup: Backup release_current'){
			steps{
				script{
					dir('orthoinference'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							sh "mysqldump -u$user -p$pass ${env.RELEASE_CURRENT} > ${env.RELEASE_CURRENT}_${env.RELEASE_NUMBER}_before_orthoinference.dump"
							sh "gzip -f ${env.RELEASE_CURRENT}_${env.RELEASE_NUMBER}_before_orthoinference.dump"
						}
					}
				}
			}
		}			
		stage('Setup: Build jar file'){
			steps{
				script{
					dir('orthoinference'){
						sh "mvn clean compile assembly:single"
					}
				}
			}
		}
		stage('Main: Run Orthoinference'){
			steps {
				script{
					speciesList = ['mmus', 'rnor', 'cfam', 'btau', 'sscr', 'drer', 'xtro', 'ggal', 'dmel', 'cele', 'ddis', 'spom', 'scer', 'pfal']
					for (species in speciesList) {
						stage('Main: Run species inference'){
							script{
								dir('orthoinference'){
									withCredentials([file(credentialsId: 'Config', variable: 'FILE')]){
										sh "java -Xmx${env.JAVA_MEM_MAX}m -jar target/orthoinference-${env.ORTHOINFERENCE_VERSION}-jar-with-dependencies.jar $FILE ${species}"
									}
								}
							}
						}
					}
				}
			}
		}

	    	stage('Post: Backup DB'){
			steps{
				script{
					dir('orthoinference'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							sh "mysqldump -u$user -p$pass ${env.RELEASE_CURRENT} > ${env.RELEASE_CURRENT}_${env.RELEASE_NUMBER}_after_orthoinference.dump"
							sh "gzip -f ${env.RELEASE_CURRENT}_${env.RELEASE_NUMBER}_after_orthoinference.dump"
						}
					}
				}
			}
		}
		stage('Archive logs and backups'){
			steps{
				script{
					dir('orthoinference'){
						sh "mkdir -p archive/${env.RELEASE_NUMBER}/logs"
						sh "mv --backup=numbered *_${env.RELEASE_NUMBER}_*.dump.gz archive/${env.RELEASE_NUMBER}/"
						sh "gzip logs/*"
						sh "mv logs/* archive/${env.RELEASE_NUMBER}/logs/"
						sh "mkdir -p ${env.RELEASE_NUMBER}"
						sh "gzip -f *.txt"
						sh "mv *.txt.gz ${env.RELEASE_NUMBER}/"
					}
				}
			}
		}
	}
}

