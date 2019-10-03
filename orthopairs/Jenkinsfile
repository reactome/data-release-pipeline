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
        stage('Setup: Build jar file') {
            steps {
				script {
                    dir ('orthopairs') {
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]) {
                  	    	sh 'mvn clean compile assembly:single'
						}
                    }
          		}
            }
        }
	    stage('Main: Generate Orthopairs files') {
            steps {
				script {
                    dir ('orthopairs') {
						withCredentials([file(credentialsId: 'Config', variable: 'FILE')]) {
                  	    	sh 'java -jar target/orthopairs-0.0.1-SNAPSHOT-jar-with-dependencies.jar $FILE'
						}
                    }
          		}
            }
        }
    }
}
