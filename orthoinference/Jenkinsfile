pipeline {
    agent any

    stages {
		stage('Back up slice_current') {
			steps {
				script {
					dir('orthoinference') {
						sh 'touch orthoinference.txt'
					}
				}
			}
		}
    }
}
