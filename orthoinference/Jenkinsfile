pipeline {
    agent any

    stages {
		stage('Back up slice_current') {
			steps {
				script {
					dir('orthoinference') {
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', username: 'user')]) {
							sh "mysql -u$user -p$pass -e \'drop database if exists release_current; create database release_current\'"
							sh "mysqldump --opt -u$user -p$pass -hlocalhost slice_current | mysql -u$user -p$pass -hlocalhost release_current"
						}
					}
				}
			}
		}
    }
}
