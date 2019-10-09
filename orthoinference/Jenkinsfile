import groovy.json.JsonSlurper
// This Jenkinsfile is used by Jenkins to run the Orthoinference step of Reactome's release. 
// It requires that the Orthopairs and UpdateStableIdentifiers steps have been run successfully before it can be run.
pipeline{
	agent any
	
	stages{
		// This stage checks that upstream projects Orthopairs and UpdateStableIdentifier, were run successfully for their last build.
		stage('Check if Orthopairs and UpdateStableIdentifiers builds succeeded'){
			steps{
				script{
					// This queries the Jenkins API to confirm that the most recent builds of Orthopairs and UpdateStableIdentifiers were successful.
					checkUpstreamBuildsSucceeded("Orthopairs")
					checkUpstreamBuildsSucceeded("UpdateStableIdentifiers")
			    	}
	    		}
		}
		// This stage backs up the release current database before it is modified.
		stage('Setup: Backup release_current'){
			steps{
				script{
					dir('orthoinference'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							def release_current_before_orthoinference_dump = "${env.RELEASE_CURRENT}_${env.RELEASE_NUMBER}_before_orthoinference.dump"
							sh "mysqldump -u$user -p$pass ${env.RELEASE_CURRENT} > $release_current_before_orthoinference_dump"
							sh "gzip -f $release_current_before_orthoinference_dump"
						}
					}
				}
			}
		}
		// This stage builds the jar file using maven. It also runs the Main orthoinference process as a 'sub-stage'. 
		// This was due to a restriction in iterating over a list of species names. To iterate, you need to first have a 'stage > steps > script hierarchy.
		// At the script level, you can iterate over a list and then create new stages from this iteration. The choice was between an empty stage or to do a sub-stage.
		stage('Setup: Build jar file'){
			steps{
				script{
					dir('orthoinference'){
						sh "mvn clean compile assembly:single"
					}
				}
				// This script block executes the main orthoinference code one species at a time. 
				// It takes all Human Reaction instances in the database and attempts to project each Reaction to each species by
				// stripping them down to the reactio's constituent proteins, checks if the protein homolog exists for that species, and infers it in Reactome's data model. 
				// If enough proteins (>= 75%) are inferrable in a Reaction, then it is created and stored in the database for this release. This is done from scratch each time.
				script{
					speciesList = ['mmus', 'rnor', 'cfam', 'btau', 'sscr', 'drer', 'xtro', 'ggal', 'dmel', 'cele', 'ddis', 'spom', 'scer', 'pfal']
					for (species in speciesList) {
						stage("Main: Infer ${species}"){
							script{
								dir('orthoinference'){
									withCredentials([file(credentialsId: 'Config', variable: 'ConfigFile')]){
										sh "java -Xmx${env.JAVA_MEM_MAX}m -jar target/orthoinference-${env.ORTHOINFERENCE_VERSION}-jar-with-dependencies.jar $ConfigFile ${species}"
									}
								}
							}
						}
					}
				}
			}
		}
		// This stage backs up the release_current database after it is modified. 
	    	stage('Post: Backup DB'){
			steps{
				script{
					dir('orthoinference'){
						withCredentials([usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							def release_current_after_orthoinference_dump = "${env.RELEASE_CURRENT}_${env.RELEASE_NUMBER}_after_orthoinference.dump"
							sh "mysqldump -u$user -p$pass ${env.RELEASE_CURRENT} > $release_current_after_orthoinference_dump"
							sh "gzip -f $release_current_after_orthoinference_dump"
						}
					}
				}
			}
		}
		// This stage archives all logs and database backups produced by Orthoinference. It also archives the eligible/inferred files produced by orthoinference.
		stage('Archive logs and backups'){
			steps{
				script{
					dir('orthoinference'){
						sh "mkdir -p archive/${env.RELEASE_NUMBER}/logs"
						sh "mv --backup=numbered *_${env.RELEASE_NUMBER}_*.dump.gz archive/${env.RELEASE_NUMBER}/"
						sh "gzip logs/*"
						sh "mv logs/* archive/${env.RELEASE_NUMBER}/logs/"
						sh "mkdir -p ${env.RELEASE_NUMBER}"
						sh "gzip -f *.txt"
						sh "mv *.txt.gz ${env.RELEASE_NUMBER}/"
					}
				}
			}
		}
	}
}

def checkUpstreamBuildsSucceeded(String stepName) {
	def statusUrl = httpRequest authentication: 'jenkinsKey', url: "${env.JENKINS_JOB_URL}/job/${env.RELEASE_NUMBER}/job/$stepName/lastBuild/api/json"
	def statusJson = new JsonSlurper().parseText(statusUrl.getContent())
	if(statusJson['result'] != "SUCCESS"){
		error("Most recent $stepName build status: " + statusJson['result'])
	}
}
