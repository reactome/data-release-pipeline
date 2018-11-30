package org.reactome.orthoinference;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.SchemaClass;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.reactome.release.common.database.InstanceEditUtils;

/**
 *
 * @author jcook
 * 
 * The Java version of infer_events.pl -- The gist of this module is that it looks at all existing Human ReactionlikeEvent (RlE) instances (mostly Reactions and BlackBoxEvents) in the Test_Reactome database,
 * and attempts to computationally infer them in each of Reactome's model organisms. Each RlE is broken down into its primary components (input, output, catalyst, and regulator), which are themselves broken
 * into their PhysicalEntity subunits. The homology data used for the inference process currently comes from Ensembl Compara and is generated during the 'Orthopairs' step of the Reactome release process.
 * After all inference attempts for each RlE has been completed in an organism, the pathways that contain the reactions are filled with these newly inferred ones. 
 * 
 *
 */

// To Reviewers: I recommend following the code sequentially, rather then looking at each class separately.
public class InferEvents
{
	static MySQLAdaptor dbAdaptor = null;
	private static GKInstance instanceEdit;
	private static GKInstance speciesInst;
	private static HashMap<GKInstance,GKInstance> manualEventToNonHumanSource = new HashMap<GKInstance,GKInstance>();
	private static ArrayList<GKInstance> manualHumanEvents = new ArrayList<GKInstance>();

	@SuppressWarnings("unchecked")
	public static void eventInferrer(Properties props, String pathToConfig, String pathToSpeciesConfig, String species) throws Exception
	{
		// Set up DB adaptor using config.properties file
		String speciesToInferFromLong = "Homo sapiens";
		String username = props.getProperty("username");
		String password = props.getProperty("password");
		String database = props.getProperty("database");
		String host = props.getProperty("host");
		int port = Integer.valueOf(props.getProperty("port"));
		
		dbAdaptor = new MySQLAdaptor(host, database, username, password, port);
		// Statically store the adaptor variable in each class
		InferReaction.setAdaptor(dbAdaptor);
		SkipTests.setAdaptor(dbAdaptor);
		InstanceUtilities.setAdaptor(dbAdaptor);
		OrthologousEntity.setAdaptor(dbAdaptor);
		InferEWAS.setAdaptor(dbAdaptor);
		UpdateHumanEvents.setAdaptor(dbAdaptor);
		
		SkipTests.getSkipList("normal_event_skip_list.txt");
		// For now an array of species names are used. I will likely change it so that the wrapper calls each organism individually during release.
//		List<String> speciesList = new ArrayList<String>(Arrays.asList("pfal", "spom", "scer", "ddis", "cele", "sscr", "btau", "cfam", "mmus", "rnor", "ggal", "tgut", "xtro", "drer", "dmel", "atha", "osat"));

		JSONParser parser = new JSONParser();
		Object obj = parser.parse(new FileReader(pathToSpeciesConfig));
		JSONObject jsonObject = (JSONObject) obj;
//		for (String species : speciesList)
//		{
			// Proper logging will be implemented before Release
			JSONObject speciesObject = (JSONObject) jsonObject.get(species);
			JSONArray speciesNames = (JSONArray) speciesObject.get("name");
			String speciesName = (String) speciesNames.get(0);
			System.out.println("Beginning orthoinference of " + speciesName + ".");
			JSONObject refDb = (JSONObject) speciesObject.get("refdb");
			String refDbUrl = (String) refDb.get("url");
			String refDbProteinUrl = (String) refDb.get("access");
			String refDbGeneUrl = (String) refDb.get("ensg_access");

			JSONObject altRefDb = (JSONObject) speciesObject.get("alt_refdb");

			// Creates two files that a) list reactions that are eligible for inference and b) those that are successfully inferred
			String eligibleFilename = "eligible_" + species	+ "_75.txt";
			String inferredFilename = "inferred_" + species + "_75.txt";
			PrintWriter eligibleFile = new PrintWriter(eligibleFilename);
			PrintWriter inferredFile = new PrintWriter(inferredFilename);
			eligibleFile.close();
			inferredFile.close();
			InferReaction.setEligibleFilename(eligibleFilename);
			InferReaction.setInferredFilename(inferredFilename);

			// Set static variables (DB/Species Instances, mapping files) that will be repeatedly used
			InferEvents.setInstanceEdits();
			try {
				HashMap<String,String[]> homologueMappings = InferEvents.readHomologueMappingFile(species, "hsap");
				ProteinCount.setHomologueMappingFile(homologueMappings);
				InferEWAS.setHomologueMappingFile(homologueMappings);
				homologueMappings = new HashMap<String,String[]>();
			} catch (FileNotFoundException e) {
				System.out.println("Unable to locate " + speciesName +" mapping file: hsap_" + species + "_mapping.txt. Orthology prediction not possible.");
//				continue;
			}
			InferEWAS.readENSGMappingFile(species);
			InferEWAS.createUniprotDbInst();
			InferEWAS.createEnsemblProteinDbInst(speciesName, refDbUrl, refDbProteinUrl);
			InferEWAS.createEnsemblGeneDBInst(speciesName, refDbUrl, refDbGeneUrl);
			if (altRefDb != null)
			{
				JSONArray altRefDbNames = (JSONArray) altRefDb.get("dbname");
				String altRefDbUrl = (String) altRefDb.get("url");
				String altRefDbAccess = (String) altRefDb.get("access");
				InferEWAS.createAlternateReferenceDBInst(speciesName, (String) altRefDbNames.get(0), altRefDbUrl, altRefDbAccess);
				if (altRefDb.get("alt_id") != null) {
					String altRefDbId = (String) altRefDb.get("alt_id");
					InferEWAS.setAlternateDBId(altRefDbId);
				}
			} else {
				InferEWAS.updateRefDb();
			}
			InferEvents.createSpeciesInst(speciesName);
			OrthologousEntity.setSpeciesInst(speciesInst);
			InferEWAS.setSpeciesInst(speciesInst);
			InstanceUtilities.setSpeciesInst(speciesInst);
			InferEvents.setSummationInst();
			InferEvents.setEvidenceTypeInst();
			OrthologousEntity.setComplexSummationInst();

/**
 *  Start of ReactionlikeEvent inference. Retrieves all human ReactionlikeEvents, and attempts to infer each for the species.
 */
		// Gets DB instance of source species (human)
		Collection<GKInstance> sourceSpeciesInst = (Collection<GKInstance>) dbAdaptor.fetchInstanceByAttribute("Species", "name", "=", speciesToInferFromLong);
		if (!sourceSpeciesInst.isEmpty())
		{
			String dbId = sourceSpeciesInst.iterator().next().getDBID().toString();
			// Gets Reaction instances of source species (human)
			Collection<GKInstance> reactionInstances = (Collection<GKInstance>) dbAdaptor.fetchInstanceByAttribute("ReactionlikeEvent", "species", "=", dbId);

			ArrayList<Long> dbids = new ArrayList<Long>();
			HashMap<Long, GKInstance> reactionMap = new HashMap<Long, GKInstance>();
			for (GKInstance reactionInst : reactionInstances) {
				dbids.add(reactionInst.getDBID());
				reactionMap.put(reactionInst.getDBID(), reactionInst);
			}
			// For now sort the instances by DB ID so that it matches the Perl sequence
			Collections.sort(dbids);
			
			if (!reactionInstances.isEmpty())
			{
				for (Long dbid : dbids)
				{
					GKInstance reactionInst = reactionMap.get(dbid);
					// Check if the current Reaction already exists for this species, that it is a valid instance (passes some filters), and that it doesnt have a Disease attribute.
					// Adds to manualHumanEvents array if it passes conditions. This code block allows you to re-run the code without re-inferring instances.
					ArrayList<GKInstance> previouslyInferredInstances = new ArrayList<GKInstance>();
					for (GKInstance orthoEventInst : (Collection<GKInstance>) reactionInst.getAttributeValuesList(ReactomeJavaConstants.orthologousEvent))
					{
						GKInstance reactionSpeciesInst = (GKInstance) orthoEventInst.getAttributeValue(ReactomeJavaConstants.species);
						if (reactionSpeciesInst.getDBID() == speciesInst.getDBID() && orthoEventInst.getAttributeValue(ReactomeJavaConstants.isChimeric) == null)
						{
							previouslyInferredInstances.add(orthoEventInst);
							
						}
					}
					for (GKInstance inferredFromInst : (Collection<GKInstance>) reactionInst.getAttributeValuesList(ReactomeJavaConstants.inferredFrom))
					{
						GKInstance reactionSpeciesInst = (GKInstance) inferredFromInst.getAttributeValue(ReactomeJavaConstants.species);
						if (reactionSpeciesInst.getDBID() == speciesInst.getDBID() && inferredFromInst.getAttributeValue(ReactomeJavaConstants.isChimeric) == null)
						{
							previouslyInferredInstances.add(inferredFromInst);
						}
					}
					if (previouslyInferredInstances.size() > 0)
					{
						GKInstance prevInfInst = previouslyInferredInstances.get(0);
						if (prevInfInst.getAttributeValue(ReactomeJavaConstants.disease) == null)
						{
							manualEventToNonHumanSource.put(reactionInst, prevInfInst);
							manualHumanEvents.add(reactionInst);
						} else {
							System.out.println("Skipping building of hierarchy around pre-existing disease reaction " + prevInfInst);
						}
						
						continue;
					}
					// This Reaction doesn't already exist for this species, and an orthologous inference will be attempted.
					System.out.println("\t" + reactionInst);
					try {
						InferReaction.reactionInferrer(reactionInst);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		UpdateHumanEvents.setInferredEvent(InferReaction.getInferredEvent());
		UpdateHumanEvents.updateHumanEvents(InferReaction.getInferrableHumanEvents());
		InferEvents.outputReport(species);
		InferEvents.resetVariables();
		System.gc();
		System.out.println("Finished orthoinference of " + speciesName + ".");
//		}
	}

	public static void outputReport(String species) throws IOException
	{
		int[] counts = InferReaction.getCounts();
		int percent = 100*counts[1]/counts[0];
		// TODO: Config out the file name
		PrintWriter reportFile = new PrintWriter("report_ortho_inference_test_reactome_67.txt");
		reportFile.close();
		String results = "hsap to " + species + ":\t" + counts[1] + " out of " + counts[0] + " eligible reactions (" + percent + "%)\n";
		Files.write(Paths.get("report_ortho_inference_test_reactome_67.txt"), results.getBytes(), StandardOpenOption.APPEND);
	}

	// Reduce memory usage after species inference complete
	public static void resetVariables() {
		InferReaction.resetVariables();
		OrthologousEntity.resetVariables();
		InferEWAS.resetVariables();
		ProteinCount.resetVariables();
		InstanceUtilities.resetVariables();
		UpdateHumanEvents.resetVariables();
		manualEventToNonHumanSource = new HashMap<GKInstance,GKInstance>();
		manualHumanEvents = new ArrayList<GKInstance>();
	}

	// Read the species-specific orthopair 'mapping' file, and create a HashMap with the contents
	public static HashMap<String, String[]> readHomologueMappingFile(String toSpecies, String fromSpecies) throws IOException
	{
		String mappingFileName = fromSpecies + "_" + toSpecies + "_mapping.txt";
		String mappingFilePath = "src/main/resources/orthopairs/" + mappingFileName;
		FileReader fr = new FileReader(mappingFilePath);
		BufferedReader br = new BufferedReader(fr);

		String currentLine;
		HashMap<String, String[]> homologueMappings = new HashMap<String,String[]>();
		while ((currentLine = br.readLine()) != null)
		{
			String[] tabSplit = currentLine.split("\t");
			String mapKey = tabSplit[0];
			String[] spaceSplit = tabSplit[1].split(" ");
			homologueMappings.put(mapKey, spaceSplit);
		}
		br.close();
		fr.close();
		return homologueMappings;
	}

	// Find the instance specific to this species
	public static void createSpeciesInst(String toSpeciesLong) throws Exception
	{
		SchemaClass referenceDb = dbAdaptor.getSchema().getClassByName(ReactomeJavaConstants.Species);
		speciesInst = new GKInstance(referenceDb);
		speciesInst.setDbAdaptor(dbAdaptor);
		speciesInst.addAttributeValue(ReactomeJavaConstants.created, instanceEdit);
		speciesInst.addAttributeValue(ReactomeJavaConstants.name, toSpeciesLong);
		speciesInst.addAttributeValue(ReactomeJavaConstants._displayName, toSpeciesLong);
		speciesInst = InstanceUtilities.checkForIdenticalInstances(speciesInst);
	}
	// Create and set static Summation instance
	public static void setSummationInst() throws Exception
	{
		GKInstance summationInst = new GKInstance(dbAdaptor.getSchema().getClassByName(ReactomeJavaConstants.Summation));
		summationInst.setDbAdaptor(dbAdaptor);
		summationInst.addAttributeValue(ReactomeJavaConstants.created, instanceEdit);
		String summationText = "This event has been computationally inferred from an event that has been demonstrated in another species.<p>The inference is based on the homology mapping in Ensembl Compara. Briefly, reactions for which all involved PhysicalEntities (in input, output and catalyst) have a mapped orthologue/paralogue (for complexes at least 75% of components must have a mapping) are inferred to the other species. High level events are also inferred for these events to allow for easier navigation.<p><a href='/electronic_inference_compara.html' target = 'NEW'>More details and caveats of the event inference in Reactome.</a> For details on the Ensembl Compara system see also: <a href='http://www.ensembl.org/info/docs/compara/homology_method.html' target='NEW'>Gene orthology/paralogy prediction method.</a>";
		summationInst.addAttributeValue(ReactomeJavaConstants.text, summationText);
		summationInst.addAttributeValue(ReactomeJavaConstants._displayName, summationText);
		summationInst = InstanceUtilities.checkForIdenticalInstances(summationInst);
		
		InferReaction.setSummationInst(summationInst);
		UpdateHumanEvents.setSummationInst(summationInst);
	}
	// Create and set static EvidenceType instance
	public static void setEvidenceTypeInst() throws Exception
	{
		GKInstance evidenceTypeInst = new GKInstance(dbAdaptor.getSchema().getClassByName(ReactomeJavaConstants.EvidenceType));
		evidenceTypeInst.setDbAdaptor(dbAdaptor);
		evidenceTypeInst.addAttributeValue(ReactomeJavaConstants.created, instanceEdit);
		String evidenceTypeText = "inferred by electronic annotation";
		evidenceTypeInst.addAttributeValue(ReactomeJavaConstants.name, evidenceTypeText);
		evidenceTypeInst.addAttributeValue(ReactomeJavaConstants.name, "IEA");
		evidenceTypeInst.addAttributeValue(ReactomeJavaConstants._displayName, evidenceTypeText);
		evidenceTypeInst = InstanceUtilities.checkForIdenticalInstances(evidenceTypeInst);
		InferReaction.setEvidenceTypeInst(evidenceTypeInst);
		UpdateHumanEvents.setEvidenceTypeInst(evidenceTypeInst);
	}
	
	public static void setInstanceEdits() throws Exception {
		instanceEdit = InstanceEditUtils.createInstanceEdit(dbAdaptor, 8948690, "org.reactome.orthoinference");
		InstanceUtilities.setInstanceEdit(instanceEdit);
		OrthologousEntity.setInstanceEdit(instanceEdit);
		InferEWAS.setInstanceEdit(instanceEdit);
		UpdateHumanEvents.setInstanceEdit(instanceEdit);
	}
}