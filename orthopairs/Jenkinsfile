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

			def releaseNumber
			def userInput = input(
				id: 'userInput', message: 'What is the release number?',
				parameters: [
            [$class: 'TextParameterDefinition', defaultValue: '', description: 'Release Version', name: 'ReleaseNumber']
            ])
			echo("The release number: " + userInput)
		}
            }
        }
    }
}
