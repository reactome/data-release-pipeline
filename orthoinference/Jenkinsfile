pipeline{
    agent any

    stages{
		stage('Setup: Create release_current from slice_current'){
			steps{
				script{
					dir('orthoinference'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]) {
							sh "mysql -u$user -p$pass -e \'drop database if exists release_current; create database release_current\'"
							sh "mysqldump --opt -u$user -p$pass -hlocalhost slice_current | mysql -u$user -p$pass -hlocalhost release_current"
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
							sh "java -jar target/orthoinference-0.0.2-SNAPSHOT-jar-with-dependencies.jar mmus"
						}
					}
				}
			}
		}
	}
}
