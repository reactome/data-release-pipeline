// This Jenkinsfile is used by Jenkins to run the OrthoinferenceStableIdentifierHistory step of Reactome's release.
// It requires that the Orthoinference step has been run successfully before it can be run.

import org.reactome.release.jenkins.utilities.Utilities

// Shared library maintained at 'release-jenkins-utils' repository.
def utils = new Utilities()

pipeline {
	agent any

	stages {
		// This stage checks that an upstream project, Orthoinference, was run successfully for its last build.
 		stage('Check if Orthoinference build succeeded'){
 			steps{
 				script{
 					utils.checkUpstreamBuildsSucceeded("Relational-Database-Updates/job/Orthoinference")
 				}
 			}
 		}
		stage('Setup: Download stable_id_mapping.stored_data.zip from S3'){
		    steps{
    			script{
    				dir('ortho-stable-id-history'){
    					sh "aws s3 --no-progress cp --recursive ${env.S3_RELEASE_DIRECTORY_URL}/stable_id_mapping.stored_data.zip ."
    				}
			    }
			}
		}
		stage('Setup: Back up DBs'){
			steps{
				script{
					dir('ortho-stable-id-history'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							utils.takeDatabaseDumpAndGzip("${env.RELEASE_CURRENT_DB}", "ortho_stable_id_history", "before", "${env.RELEASE_SERVER}")
							utils.takeDatabaseDumpAndGzip("${env.STABLE_IDENTIFIERS_DB}", "ortho_stable_id_history", "before", "${env.RELEASE_SERVER}")
						}
					}
				}
			}
		}
		stage('Setup: Build jar file'){
			steps{
				script{
					dir('ortho-stable-id-history'){
						utils.buildJarFile()
					}
				}
			}
		}
		stage('Main: Save StableIdentifier History'){
			steps{
				script{
					dir('ortho-stable-id-history'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							def releaseVersion = utils.getReleaseVersion()
							sh  "perl ${env.ABS_RELEASE_PATH}/generate_stable_ids_orthoinference/save_stable_id_history.pl -db ${env.RELEASE_CURRENT_DB} -sdb ${env.STABLE_IDENTIFIERS_DB} -host localhost -user $user -pass $pass -release ${releaseVersion}" 
						}
					}
				}
			}
		}
		stage('Main: Old StableIdentifier Mapping'){
			steps{
				script{
					dir('ortho-stable-id-history'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							sh "perl ${env.ABS_RELEASE_PATH}/generate_stable_ids_orthoinference/old_stable_id_mapping.pl -db ${env.RELEASE_CURRENT_DB} -host localhost"
						}
					}
				}
			}
		}
		stage('Post: Run StableIdentifier QA'){
			steps{
				script{
					dir('ortho-stable-id-history') {
						withCredentials([file(credentialsId: 'Config', variable: 'ConfigFile')]) {
							sh "java -jar target/OrthoStableIdHistory-*-jar-with-dependencies.jar $ConfigFile"
						}
					}
				}
			}
		}
		stage('Post: Backup DBs'){
			steps{
				script{
					dir('ortho-stable-id-history'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							utils.takeDatabaseDumpAndGzip("${env.RELEASE_CURRENT_DB}", "ortho_stable_id_history", "after", "${env.RELEASE_SERVER}")
							utils.takeDatabaseDumpAndGzip("${env.STABLE_IDENTIFIERS_DB}", "ortho_stable_id_history", "after", "${env.RELEASE_SERVER}")

						}
					}
				}
			}
		}
		// This stage archives all logs and database backups produced by OrthoinferenceStableIdentifierHistory.
		stage('Post: Archive Outputs'){
			steps{
				script{
					dir('ortho-stable-id-history'){
						def s3Path = "${env.S3_RELEASE_DIRECTORY_URL}/${currentRelease}/ortho_stable_id_history"
						sh "mkdir -p databases/"
						sh "mv --backup=numbered *_${currentRelease}_*.dump.gz databases/"
						sh "mv ${env.ABS_RELEASE_PATH}/generate_stable_ids_orthoinference/*.log logs/"
						sh "mv ${env.ABS_RELEASE_PATH}/generate_stable_ids_orthoinference/*.err logs/"
						sh "gzip -r logs/*"
						sh "aws s3 --no-progress --recursive cp databases/ $s3Path/databases/"
						sh "aws s3 --no-progress --recursive cp logs/ $s3Path/logs/"
						sh "rm -r logs databases stable_id_mapping.stored_data*"
					}
				}
			}
		}
	}		
}
