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
		// Backs up the release_current and stable_identifiers databases.
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
		// Builds the jar file that will be used.
		stage('Setup: Build jar file'){
			steps{
				script{
					dir('ortho-stable-id-history'){
						utils.buildJarFile()
					}
				}
			}
		}
		// Runs the perl 'save_stable_id_history.pl' script.
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
		// Runs the perl 'old_stable_id_mapping.pl' script.
		stage('Main: Old StableIdentifier Mapping'){
			steps{
				script{
					dir('ortho-stable-id-history'){
						// Download 'stable_id_mapping.stored_data' from S3
						sh "aws s3 cp ${env.S3_RELEASE_DIRECTORY_URL}/supplementary_files/stable_id_mapping.stored_data.zip ."
						sh "unzip stable_id_mapping.stored_data.zip"
						// Ensure that the file is in the same location as the Perl script
						sh "cp ./stable_id_mapping.stored_data ${env.ABS_RELEASE_PATH}/generate_stable_ids_orthoinference/stable_id_mapping.stored_data"
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							sh "perl ${env.ABS_RELEASE_PATH}/generate_stable_ids_orthoinference/old_stable_id_mapping.pl -db ${env.RELEASE_CURRENT_DB} -host localhost"
						}
						sh "rm stable_id_mapping.stored_data*"
					}
				}
			}
		}
		// Runs the StableIdentifier QA.
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
		// Backs up the 'release_current' and 'stable_identifier' databases after the step has run.
		stage('Post: Backup DBs'){
			steps{
				script{
					dir('ortho-stable-id-history'){
						// Download 'stable_id_mapping.stored_data' from S3
						def stableIdMappingStorFile = "stable_id_mapping.stored_data"
						sh "aws s3 --no-progress cp ${env.S3_RELEASE_DIRECTORY_URL}/supplementary_files/${stableIdMappingStorFile}.zip ."
						sh "unzip ${stableIdMappingStorFile}.zip"
						sh "mv ${stableIdMappingStorFile} ${env.ABS_RELEASE_PATH}/generate_stable_ids_orthoinference/"
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							sh "perl ${env.ABS_RELEASE_PATH}/generate_stable_ids_orthoinference/old_stable_id_mapping.pl -db ${env.RELEASE_CURRENT_DB} -host localhost"
						}
						sh "rm ${stableIdMappingStorFile}.zip"
						sh "rm ${env.ABS_RELEASE_PATH}/generate_stable_ids_orthoinference/${stableIdMappingStorFile}"
					}
				}
			}
		}
		// This stage archives all logs and database backups produced by OrthoinferenceStableIdentifierHistory.
		stage('Post: Archive Outputs'){
			steps{
				script{
					dir('ortho-stable-id-history'){
						def dataFiles = []
						def logFiles = ["${env.ABS_RELEASE_PATH}/generate_stable_ids_orthoinference/*log", "${env.ABS_RELEASE_PATH}/generate_stable_ids_orthoinference/*err"]
						def foldersToDelete = []
						utils.cleanUpAndArchiveBuildFiles("ortho_stable_id_history", dataFiles, logFiles, foldersToDelete)
					}
				}
			}
		}
	}
}
