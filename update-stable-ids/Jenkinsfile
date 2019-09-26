pipeline {
  agent any
    stages {
		stage('Setup: Rotate slice DBs'){
			steps{
				script{
					dir('update-stable-ids'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							sh "mysql -u$user -p$pass -e \'drop database if exists ${env.SLICE_PREVIOUS}; create database ${env.SLICE_PREVIOUS}\'"
							sh "zcat  archive/${env.PREV_RELEASE_NUMBER}/test_slice_${env.PREV_RELEASE_NUMBER}_snapshot.dump.gz 2>&1 | mysql -u$user -p$pass -hlocalhost ${env.SLICE_PREVIOUS}"
							sh "mysqldump -u$user -p$pass ${env.SLICE_TEST} > test_slice_${env.RELEASE_NUMBER}_snapshot.dump"
							sh "gzip -f test_slice_${env.RELEASE_NUMBER}_snapshot.dump"
							sh "mysql -u$user -p$pass -e \'drop database if exists ${env.SLICE_CURRENT}; create database ${env.SLICE_CURRENT}\'"
							sh "zcat  test_slice_${env.RELEASE_NUMBER}_snapshot.dump.gz 2>&1 | mysql -u$user -p$pass -hlocalhost ${env.SLICE_CURRENT}"
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
						    sh "mysqldump -u$user -p$pass gk_central > gk_central_${env.RELEASE_NUMBER}_before_st_id.dump"
							sh "gzip -f gk_central_${env.RELEASE_NUMBER}_before_st_id.dump"
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
							sh "mysqldump -u$user -p$pass ${env.GK_CENTRAL} > gk_central_${env.RELEASE_NUMBER}_after_st_id.dump"
							sh "gzip -f gk_central_${env.RELEASE_NUMBER}_after_st_id.dump"
							sh "mysqldump -u$user -p$pass ${env.SLICE_CURRENT} > slice_current_${env.RELEASE_NUMBER}_after_st_id.dump"
							sh "gzip -f slice_current_${env.RELEASE_NUMBER}_after_st_id.dump"
						}
					}
				}
			}
		}
		stage('Archive logs and backups'){
			steps{
				script{
					dir('update-stable-ids'){
						sh "mkdir -p archive/${env.RELEASE_NUMBER}"
						sh "mv --backup=numbered *_${env.RELEASE_NUMBER}_*.dump.gz archive/${env.RELEASE_NUMBER}/"
						sh "gzip logs/*"
						sh "mv logs/* archive/${env.RELEASE_NUMBER}"
					}
				}
			}
		}
	}
}
