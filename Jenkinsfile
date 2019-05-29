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
	       cd orthopairs
	       sh 'bash updateOrthopairsConfig.sh -r 69'
	       sh 'mvn clean compile assembly:single'
            }
        }
    }
}
