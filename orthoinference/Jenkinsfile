pipeline {
    agent any

    stages {
		dir('orthoinference') {
			stage('Back up DB') {
				steps {
					script {
						sh 'touch orthoinference.txt'
					}
				}
			}
		}
    }
}
