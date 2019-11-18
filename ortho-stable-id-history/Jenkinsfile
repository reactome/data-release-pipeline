
import groovy.json.JsonSlurper
// This Jenkinsfile is used by Jenkins to run the Orthoinference step of Reactome's release.
// It requires that the Orthopairs and UpdateStableIdentifiers steps have been run successfully before it can be run.
def currentRelease
def previousRelease
pipeline {
	agent any

	stages {
		// This stage checks that an upstream project, Orthoinference, was run successfully for its last build.
		stage('Check if Orthoinference build succeeded'){
			steps{
				script{
					currentRelease = (pwd() =~ /Releases\/(\d+)\//)[0][1];
					previousRelease = (pwd() =~ /Releases\/(\d+)\//)[0][1].toInteger() - 1;
					// This queries the Jenkins API to confirm that the most recent build of Orthoinference was successful.
					def orthoinferenceUrl = httpRequest authentication: 'jenkinsKey', validResponseCodes: "${env.VALID_RESPONSE_CODES}", url: "${env.JENKINS_JOB_URL}/job/$currentRelease/job/Orthoinference/lastBuild/api/json"
					if (orthoinferenceUrl.getStatus() == 404) {
						error("Orthoinference has not yet been run. Please complete a successful build.")
					} else {
						def orthoinferenceJson = new JsonSlurper().parseText(orthoinferenceUrl.getContent())
						if(orthoinferenceJson['result'] != "SUCCESS"){
							error("Most recent Orthoinference build status: " + orthoinferenceJson['result'])
						}
					}
				}
			}
		}
		stage('Setup: Back up DBs'){
			steps{
				script{
					dir('ortho-stable-id-history'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							def release_current_before_ortho_stable_id_history_dump = "${env.RELEASE_CURRENT}_${currentRelease}_before_ortho_stable_id_history.dump"
							def stable_identifiers_before_ortho_stable_id_history_dump = "${env.STABLE_IDENTIFIERS}_${currentRelease}_before_ortho_stable_id_history.dump"
							sh "mysqldump -u$user -p$pass ${env.RELEASE_CURRENT} > $release_current_before_ortho_stable_id_history_dump"
							sh "gzip -f $release_current_before_ortho_stable_id_history_dump"
							sh "mysqldump -u$user -p$pass ${env.STABLE_IDENTIFIERS} > $stable_identifiers_before_ortho_stable_id_history_dump"
							sh "gzip -f $stable_identifiers_before_ortho_stable_id_history_dump"
						}
					}
				}
			}
		}
		stage('Setup: Build jar file'){
			steps{
				script{
					dir('ortho-stable-id-history'){
						sh 'mvn clean compile assembly:single'
					}
				}
			}
		}
		stage('Main: Save StableIdentifier History'){
			steps{
				script{
					dir('ortho-stable-id-history'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							sh  "perl ${env.ABS_ORTHO_STABLE_IDENTIFIER_HISTORY_PATH}/generate_stable_ids_orthoinference/save_stable_id_history.pl -db ${env.RELEASE_CURRENT} -sdb ${env.STABLE_IDENTIFIERS} -host localhost -user $user -pass $pass -release ${currentRelease} > old_stable_id_mapping_$version.out 2> old_stable_id_mapping_$version.err"
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
							sh "perl ${env.ABS_RELEASE_PATH}/generate_stable_ids_orthoinference/old_stable_id_mapping.pl -db ${env.RELEASE_CURRENT} -host localhost > old_stable_id_mapping_$version.out 2> old_stable_id_mapping_$version.err"
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
							sh "java -jar target/OrthoStableIdHistory-${env.ORTHO_STABLE_IDS_HISTORY_VERSION}-jar-with-dependencies.jar $ConfigFile"
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
							def release_current_after_ortho_stable_id_history_dump = "${env.RELEASE_CURRENT}_${currentRelease}_after_ortho_stable_id_history.dump"
							def stable_identifiers_after_ortho_stable_id_history_dump = "${env.STABLE_IDENTIFIERS}_${currentRelease}_after_ortho_stable_id_history.dump"
							sh "mysqldump -u$user -p$pass ${env.RELEASE_CURRENT} > $release_current_after_ortho_stable_id_history_dump"
							sh "gzip -f $release_current_after_ortho_stable_id_history_dump"
							sh "mysqldump -u$user -p$pass ${env.STABLE_IDENTIFIERS} > $stable_identifiers_after_ortho_stable_id_history_dump"
							sh "gzip -f $stable_identifiers_after_ortho_stable_id_history_dump"
						}
					}
				}
			}
		}
		stage('Post: Archive logs and backups'){
			steps{
				script{
					dir('ortho-stable-id-history'){
						sh "mkdir -p archive/${currentRelease}/logs"
						sh "mv --backup=numbered *_${currentRelease}_*.dump.gz archive/${currentRelease}/"
						sh "gzip logs/*"
						sh "mv logs/* archive/${currentRelease}/logs/"
					}
				}
			}
		}
	}		
}
