pipeline {
    agent {
        docker {
            image 'maven:3-alpine'
            args '-v /root/.m2:/root/.m2'
        }
    }
    stages {
        stage('Build') {
            steps {
		script {
          			def userInput = input(
          				id: 'userInput', message: 'What is the release number?',
          				parameters: [
                      [$class: 'TextParameterDefinition', defaultValue: '', description: 'Release Version', name: 'ReleaseNumber']
                      ])
          			echo("The release number: " + userInput)
                dir ('orthopairs') {
                  sh 'bash updateOrthopairsConfig.sh -r 69'
                  sh 'mvn clean compile assembly:single'
                  sh 'java -jar target/orthopairs-0.0.1-SNAPSHOT-jar-with-dependencies.jar'
                }
          		}
            }
        }
    }
}
