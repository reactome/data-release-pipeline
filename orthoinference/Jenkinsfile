import groovy.json.JsonSlurper

pipeline{
    agent any
	
    stages{
	    stage('Check if upstream build success'){
		    steps{
			    script{
				    def statusUrl = httpRequest authentication: 'jenkinsKey', url: "${env.JENKINS_JOB_URL}Release/job/Orthopairs/lastBuild/api/json"
					def statusJson = new JsonSlurper().parseText(statusUrl.getContent())
					if(statusJson['result'] != "SUCCESS"){
						error("Most recent Orthopairs build status: " + statusJson['result'])
					} 
			    }
		    }
	    }
		stage('Setup: Create release_current from slice_current'){
			steps{
				script{
					dir('orthoinference'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]) {
							sh "mysql -u$user -p$pass -e \'drop database if exists ${env.RELEASE_CURRENT}; create database ${env.RELEASE_CURRENT}\'"
							sh "mysqldump --opt -u$user -p$pass -hlocalhost ${env.SLICE_CURRENT} | mysql -u$user -p$pass -hlocalhost ${env.RELEASE_CURRENT}"
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
	    
	}
}

