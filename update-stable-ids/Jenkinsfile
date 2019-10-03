import groovy.json.JsonSlurper

pipeline {
	agent any
	
	stages {
		stage('Check upstream builds succeeded'){
			steps{
				script{
					def configStatusUrl = httpRequest authentication: 'jenkinsKey', url: "${env.JENKINS_JOB_URL}/job/${env.RELEASE_NUMBER}/job/ConfirmReleaseConfigs/lastBuild/api/json"
					def configStatusJson = new JsonSlurper().parseText(configStatusUrl.getContent())
					if(configStatusJson['result'] != "SUCCESS"){
						error("Most recent ConfirmReleaseConfigs build status: " + configStatusJson['result'] + ". Please complete a successful build.")
					}
				}	
			}
		}
		stage('Setup: Rotate slice DBs'){
			steps{
				script{
					dir('update-stable-ids'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							sh "mysql -u$user -p$pass -e \'drop database if exists ${env.SLICE_PREVIOUS}; create database ${env.SLICE_PREVIOUS}\'"
							sh "zcat  archive/${env.PREV_RELEASE_NUMBER}/${env.SLICE_TEST}_${env.PREV_RELEASE_NUMBER}_snapshot.dump.gz 2>&1 | mysql -u$user -p$pass -hlocalhost ${env.SLICE_PREVIOUS}"
							sh "mysqldump -u$user -p$pass ${env.SLICE_TEST} > ${env.SLICE_TEST}_${env.RELEASE_NUMBER}_snapshot.dump"
							sh "gzip -f ${env.SLICE_TEST}_${env.RELEASE_NUMBER}_snapshot.dump"
							sh "mysql -u$user -p$pass -e \'drop database if exists ${env.SLICE_CURRENT}; create database ${env.SLICE_CURRENT}\'"
							sh "zcat  ${env.SLICE_TEST}_${env.RELEASE_NUMBER}_snapshot.dump.gz 2>&1 | mysql -u$user -p$pass -hlocalhost ${env.SLICE_CURRENT}"
						}
					}
				}
			}
		}
		stage('Setup: Back up gk_central'){
			steps{
				script{
					dir('update-stable-ids'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							sh "mysqldump -u$user -p$pass -h${env.CURATOR_SERVER} ${env.GK_CENTRAL} > ${env.GK_CENTRAL}_${env.RELEASE_NUMBER}_before_st_id.dump"
							sh "gzip -f ${env.GK_CENTRAL}_${env.RELEASE_NUMBER}_before_st_id.dump"
						}
					}
				}
			}
		}			    
		stage('Setup: Build jar file'){
			steps{
				script{
					dir('update-stable-ids'){
						sh "mvn clean compile assembly:single"
					}
				}
			}
		}
		stage('Main: Update Stable Identifiers'){
			steps {
				script{
					dir('update-stable-ids'){
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]){
							sh "java -Xmx${env.JAVA_MEM_MAX}m -jar target/updateStableIds-0.0.1-SNAPSHOT-jar-with-dependencies.jar $FILE"
						}
					}
				}
			}
		}
		stage('Post: Create release_current from slice_current'){
			steps{
				script{
					dir('update-stable-ids'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]) {
							sh "mysql -u$user -p$pass -e \'drop database if exists ${env.RELEASE_CURRENT}; create database ${env.RELEASE_CURRENT}\'"
							sh "mysqldump --opt -u$user -p$pass -hlocalhost ${env.SLICE_CURRENT} | mysql -u$user -p$pass -hlocalhost ${env.RELEASE_CURRENT}"
						}
					}
				}
			}
		}
		stage('Post: Backup DBs'){
			steps{
				script{
					dir('update-stable-ids'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							sh "mysqldump -u$user -p$pass -h${env.CURATOR_SERVER} ${env.GK_CENTRAL} > ${env.GK_CENTRAL}_${env.RELEASE_NUMBER}_after_st_id.dump"
							sh "gzip -f gk_central_${env.RELEASE_NUMBER}_after_st_id.dump"
							sh "mysqldump -u$user -p$pass ${env.SLICE_CURRENT} > ${env.SLICE_CURRENT}_${env.RELEASE_NUMBER}_after_st_id.dump"
							sh "gzip -f ${env.SLICE_CURRENT}_${env.RELEASE_NUMBER}_after_st_id.dump"
						}
					}
				}
			}
		}
		stage('Archive logs and backups'){
			steps{
				script{
					dir('update-stable-ids'){
						sh "mkdir -p archive/${env.RELEASE_NUMBER}/logs"
						sh "mv --backup=numbered *_${env.RELEASE_NUMBER}_*.dump.gz archive/${env.RELEASE_NUMBER}/"
						sh "gzip logs/*"
						sh "mv logs/* archive/${env.RELEASE_NUMBER}/logs/"
					}
				}
			}
		}
	}
}
