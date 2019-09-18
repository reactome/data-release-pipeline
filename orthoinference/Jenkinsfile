pipeline {
    agent any

    stages {
		stage('Back up slice_current') {
			steps {
				script {
					dir('orthoinference') {
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]) {
							sh "mysql -u$user -p$pass -e \'drop database if exists release_current; create database release_current\'"
							sh "mysqldump --opt -u$user -p$pass -hlocalhost slice_current > slice_current_before_orthoinference.sql"
							sh "cat slice_current_before_orthoinference.sql | mysql -u$user -p$pass -hlocalhost release_current"
							sh "gzip slice_current_before_orthoinference.sql"
						}
					}
				}
			}
		}
    }
}
