import groovy.json.JsonSlurper

pipeline{
    agent any
	
    stages{
	    stage('Check if upstream builds succeeded'){
		    steps{
			    script{
				    def orthopairsStatusUrl = httpRequest authentication: 'jenkinsKey', url: "${env.JENKINS_JOB_URL}Release/job/Orthopairs/lastBuild/api/json"
					def orthopairsStatusJson = new JsonSlurper().parseText(orthopairsStatusUrl.getContent())
					if(orthopairsStatusJson['result'] != "SUCCESS"){
						error("Most recent Orthopairs build status: " + orthopairsStatusJson['result'])
					}
					def updateStIdsStatusUrl = httpRequest authentication: 'jenkinsKey', url: "${env.JENKINS_JOB_URL}Release/job/UpdateStableIdentifiers/lastBuild/api/json"
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
							sh "mysqldump -u$user -p$pass ${env.RELEASE_CURRENT} > ${env.RELEASE_CURRENT}_${env.RELEASE_NUMBER}_before_orthoinference_dois.dump"
							sh "gzip -f ${env.RELEASE_CURRENT}_${env.RELEASE_NUMBER}_before_orthoinference_dois.dump"
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
		stage('Main: Infer mmus'){
			steps {
				script{
					dir('orthoinference'){
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]){
							sh "java -jar target/orthoinference-0.0.2-SNAPSHOT-jar-with-dependencies.jar $FILE mmus"
						}
					}
				}
			}
		}
		stage('Main: Infer rnor'){
			steps {
				script{
					dir('orthoinference'){
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]){
							sh "java -jar target/orthoinference-0.0.2-SNAPSHOT-jar-with-dependencies.jar $FILE rnor"
						}
					}
				}
			}
		}
		stage('Main: Infer cfam'){
			steps {
				script{
					dir('orthoinference'){
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]){
							sh "java -jar target/orthoinference-0.0.2-SNAPSHOT-jar-with-dependencies.jar $FILE cfam"
						}
					}
				}
			}
		}
		stage('Main: Infer btau'){
			steps {
				script{
					dir('orthoinference'){
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]){
							sh "java -jar target/orthoinference-0.0.2-SNAPSHOT-jar-with-dependencies.jar $FILE btau"
						}
					}
				}
			}
		}
		stage('Main: Infer sscr'){
			steps {
				script{
					dir('orthoinference'){
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]){
							sh "java -jar target/orthoinference-0.0.2-SNAPSHOT-jar-with-dependencies.jar $FILE sscr"
						}
					}
				}
			}
		}
		stage('Main: Infer drer'){
			steps {
				script{
					dir('orthoinference'){
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]){
							sh "java -jar target/orthoinference-0.0.2-SNAPSHOT-jar-with-dependencies.jar $FILE drer"
						}
					}
				}
			}
		}
		stage('Main: Infer xtro'){
			steps {
				script{
					dir('orthoinference'){
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]){
							sh "java -jar target/orthoinference-0.0.2-SNAPSHOT-jar-with-dependencies.jar $FILE xtro"
						}
					}
				}
			}
		}
		stage('Main: Infer ggal'){
			steps {
				script{
					dir('orthoinference'){
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]){
							sh "java -jar target/orthoinference-0.0.2-SNAPSHOT-jar-with-dependencies.jar $FILE ggal"
						}
					}
				}
			}
		}
		stage('Main: Infer dmel'){
			steps {
				script{
					dir('orthoinference'){
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]){
							sh "java -jar target/orthoinference-0.0.2-SNAPSHOT-jar-with-dependencies.jar $FILE dmel"
						}
					}
				}
			}
		}
		stage('Main: Infer cele'){
			steps {
				script{
					dir('orthoinference'){
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]){
							sh "java -jar target/orthoinference-0.0.2-SNAPSHOT-jar-with-dependencies.jar $FILE cele"
						}
					}
				}
			}
		}
		stage('Main: Infer ddis'){
			steps {
				script{
					dir('orthoinference'){
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]){
							sh "java -jar target/orthoinference-0.0.2-SNAPSHOT-jar-with-dependencies.jar $FILE ddis"
						}
					}
				}
			}
		}
		stage('Main: Infer spom'){
			steps {
				script{
					dir('orthoinference'){
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]){
							sh "java -jar target/orthoinference-0.0.2-SNAPSHOT-jar-with-dependencies.jar $FILE spom"
						}
					}
				}
			}
		}
		stage('Main: Infer scer'){
			steps {
				script{
					dir('orthoinference'){
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]){
							sh "java -jar target/orthoinference-0.0.2-SNAPSHOT-jar-with-dependencies.jar $FILE scer"
						}
					}
				}
			}
		}
		stage('Main: Infer pfal'){
			steps {
				script{
					dir('orthoinference'){
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]){
							sh "java -jar target/orthoinference-0.0.2-SNAPSHOT-jar-with-dependencies.jar $FILE pfal"
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
						sh "mv --backup=numbered *_${env.RELEASE_NUMBER}_*.dump.gz archive/${env.RELEASE_NUMBER}/"
						sh "gzip logs/*"
						sh "mv logs/* archive/${env.RELEASE_NUMBER}/logs/"
					}
				}
			}
		}
	}
}

