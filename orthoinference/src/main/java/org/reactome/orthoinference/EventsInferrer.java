package org.reactome.orthoinference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import static org.gk.model.ReactomeJavaConstants.*;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
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
 * into their PhysicalEntity subunits. The homology data used for the inference process comes from PANTHER (www.pantherdb.org) and is generated during the 'Orthopairs' step of the Reactome release process.
 * After all inference attempts for each RlE has been completed in an organism, the pathways that contain the reactions are filled with these newly inferred ones. 
 * 
 *
 */

public class EventsInferrer
{
	private static final Logger logger = LogManager.getLogger();
	private static MySQLAdaptor dbAdaptor;
	private static MySQLAdaptor dbAdaptorPrev;
	private static String releaseVersion;
	private static GKInstance instanceEditInst;
	private static GKInstance speciesInst;
	private static Map<GKInstance, GKInstance> eventsAlreadyInferredMap = new HashMap<>();
	private static List<GKInstance> eventsAlreadyInferred = new ArrayList<>();
	private static StableIdentifierGenerator stableIdentifierGenerator;
	private static OrthologousPathwayDiagramGenerator orthologousPathwayDiagramGenerator;
	private static final String summationText = "This event has been computationally inferred from an event that has been demonstrated in another species.<p>The inference is based on the homology mapping from PANTHER. Briefly, reactions for which all involved PhysicalEntities (in input, output and catalyst) have a mapped orthologue/paralogue (for complexes at least 75% of components must have a mapping) are inferred to the other species. High level events are also inferred for these events to allow for easier navigation.<p><a href='/electronic_inference_compara.html' target = 'NEW'>More details and caveats of the event inference in Reactome.</a> For details on PANTHER see also: <a href='http://www.pantherdb.org/about.jsp' target='NEW'>http://www.pantherdb.org/about.jsp</a>";


	@SuppressWarnings("unchecked")
	public static void inferEvents(Properties props, String species) throws Exception
	{	
		logger.info("Preparing DB Adaptor and setting project variables");
		// Set up DB adaptor using config.properties file
		String username = props.getProperty("username");
		String password = props.getProperty("password");
		String database = props.getProperty("currentDatabase");
		String prevDatabase = props.getProperty("previousDatabase");
		String host = props.getProperty("host");
		int port = Integer.valueOf(props.getProperty("port"));
		
		dbAdaptor = new MySQLAdaptor(host, database, username, password, port);
		dbAdaptorPrev = new MySQLAdaptor(host, prevDatabase, username, password, port);
		if (dbAdaptor == null || dbAdaptorPrev == null) {
			logger.fatal("Null MySQLAdaptor, terminating orthoinference");
			return;
		}
		setDbAdaptors(dbAdaptor);

		releaseVersion = props.getProperty("releaseNumber");
		String pathToOrthopairs = props.getProperty("pathToOrthopairs");
		String pathToSpeciesConfig = props.getProperty("pathToSpeciesConfig");
		String dateOfRelease = props.getProperty("dateOfRelease");
		int personId = Integer.valueOf(props.getProperty("personId"));
		setReleaseDates(dateOfRelease);
		
		SkipInstanceChecker.getSkipList("normal_event_skip_list.txt");
		
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(new FileReader(pathToSpeciesConfig));
		JSONObject jsonObject = (JSONObject) obj;

		// Parse Species information (found in Species.json config file)
		JSONObject speciesObject = (JSONObject) jsonObject.get(species);
		JSONArray speciesNames = (JSONArray) speciesObject.get("name");
		String speciesName = (String) speciesNames.get(0);
		logger.info("Beginning orthoinference of " + speciesName);

		JSONObject refDb = (JSONObject) speciesObject.get("refdb");
		String refDbUrl = (String) refDb.get("url");
		String refDbProteinUrl = (String) refDb.get("access");
		String refDbGeneUrl = (String) refDb.get("ensg_access");
		
		// Creates two files that a) list reactions that are eligible for inference and b) those that are successfully inferred
		String eligibleFilename = "eligible_" + species	+ "_75.txt";
		String inferredFilename = "inferred_" + species + "_75.txt";
		createNewFile(eligibleFilename);
		createNewFile(inferredFilename);
		SkipInstanceChecker.setEligibleFilename(eligibleFilename);
		ReactionInferrer.setInferredFilename(inferredFilename);

		stableIdentifierGenerator = new StableIdentifierGenerator(dbAdaptor, (String) speciesObject.get("abbreviation"));
		// Set static variables (DB/Species Instances, mapping files) that will be repeatedly used
		setInstanceEdits(personId);
		try {
			Map<String,String[]> homologueMappings = readHomologueMappingFile(species, "hsap", pathToOrthopairs);
			ProteinCountUtility.setHomologueMappingFile(homologueMappings);
			EWASInferrer.setHomologueMappingFile(homologueMappings);
			SkipInstanceChecker.setHomologueMappingFile(homologueMappings);
		} catch (FileNotFoundException e) {
			logger.fatal("Unable to locate " + speciesName +" mapping file: hsap_" + species + "_mapping.txt. Orthology prediction not possible.");
			return;
		}
		EWASInferrer.readENSGMappingFile(species, pathToOrthopairs);
		EWASInferrer.fetchAndSetUniprotDbInstance();
		EWASInferrer.createEnsemblProteinDbInstance(speciesName, refDbUrl, refDbProteinUrl);
		EWASInferrer.createEnsemblGeneDBInstance(speciesName, refDbUrl, refDbGeneUrl);
		
		JSONObject altRefDbJSON = (JSONObject) speciesObject.get("alt_refdb");
		if (altRefDbJSON != null)
		{
			logger.info("Alternate DB exists for " + speciesName);
			EWASInferrer.createAlternateReferenceDBInstance(altRefDbJSON);
		} else {
			EWASInferrer.setAltRefDbToFalse();
		}
		createAndSetSpeciesInstance(speciesName);
		setSummationInstance();
		setEvidenceTypeInstance();
		OrthologousEntityGenerator.setComplexSummationInstance();

/**
 *  Start of ReactionlikeEvent inference. Retrieves all human ReactionlikeEvents, and attempts to infer each for the species.
 */
		// Gets DB instance of source species (human)
		Collection<GKInstance> sourceSpeciesInst = (Collection<GKInstance>) dbAdaptor.fetchInstanceByAttribute("Species", "name", "=", "Homo sapiens");
		if (sourceSpeciesInst.isEmpty())
		{
			logger.info("Could not find Species instance for Homo sapiens");
			return;
		}
		long humanInstanceDbId = sourceSpeciesInst.iterator().next().getDBID();
		orthologousPathwayDiagramGenerator = new OrthologousPathwayDiagramGenerator(dbAdaptor, dbAdaptorPrev, speciesInst, personId, humanInstanceDbId);
		// Gets Reaction instances of source species (human)
		Collection<GKInstance> reactionInstances = (Collection<GKInstance>) dbAdaptor.fetchInstanceByAttribute("ReactionlikeEvent", "species", "=", humanInstanceDbId);

		List<Long> dbids = new ArrayList<>();
		Map<Long, GKInstance> reactionMap = new HashMap<>();
		for (GKInstance reactionInst : reactionInstances) {
			dbids.add(reactionInst.getDBID());
			reactionMap.put(reactionInst.getDBID(), reactionInst);
		}
		Collections.sort(dbids);

		logger.info(sourceSpeciesInst.iterator().next().getDisplayName() + " ReactionlikeEvent instances: " + dbids.size());
		for (Long dbid : dbids)
		{
			GKInstance reactionInst = reactionMap.get(dbid);
			logger.info("Attempting RlE inference: " + reactionInst);
			// Check if the current Reaction already exists for this species, that it is a valid instance (passes some filters), and that it doesn't have a Disease attribute.
			// Adds to manualHumanEvents array if it passes conditions. This code block allows you to re-run the code without re-inferring instances.
			List<GKInstance> previouslyInferredInstances = new ArrayList<>();
			previouslyInferredInstances = checkIfPreviouslyInferred(reactionInst, orthologousEvent, previouslyInferredInstances);
			previouslyInferredInstances.addAll(checkIfPreviouslyInferred(reactionInst, inferredFrom, previouslyInferredInstances));
			if (previouslyInferredInstances.size() > 0) {
				GKInstance prevInfInst = previouslyInferredInstances.get(0);
				GKInstance prevInfSummationInst = (GKInstance) prevInfInst.getAttributeValue(summation);
				String prevInfSummationText = prevInfSummationInst.getAttributeValue(text).toString();
				if (prevInfInst.getAttributeValue(disease) == null &&  prevInfSummationText.equals(summationText)) {
					logger.info("Inferred RlE already exists, skipping inference");
					eventsAlreadyInferredMap.put(reactionInst, prevInfInst);
					eventsAlreadyInferred.add(reactionInst);
				} else {
					logger.info("Either a disease or manually inferred reaction, skipping inference");
				}
				continue;
			}

			// An inferred ReactionlikeEvent doesn't already exist for this species, and an orthologous inference will be attempted.
			try {
				ReactionInferrer.inferReaction(reactionInst);
				logger.info("Successfully inferred " + reactionInst);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
		// Retrieve events inferred from this run, and any that were already inferred. Combine them and then begin Pathway inference.
		// The two methods below perform this for a map, containing the original RlE and the inferred RlE, and a List of just the inferred RlEs.
		// The latter will be iterated through when building Pathway hierarchies, the former when information from original RlE is needed during this build.
		PathwaysInferrer.setInferredEvent(ReactionInferrer.getInferredEvent(eventsAlreadyInferredMap));
		PathwaysInferrer.inferPathways(ReactionInferrer.getInferrableHumanEvents(eventsAlreadyInferred));
		orthologousPathwayDiagramGenerator.generateOrthologousPathwayDiagrams();
		outputReport(species);
		logger.info("Finished orthoinference of " + speciesName);
	}

	private static void createNewFile(String filename) throws IOException {
		File file = new File(filename);
		if (file.exists()) {
			file.delete();
		}
		file.createNewFile();
	}

	public static StableIdentifierGenerator getStableIdentifierGenerator() {
		return stableIdentifierGenerator;
	}

	private static void setReleaseDates(String dateOfRelease) 
	{
		ReactionInferrer.setReleaseDate(dateOfRelease);
		PathwaysInferrer.setReleaseDate(dateOfRelease);
	
	}

	@SuppressWarnings("unchecked")
	private static List<GKInstance> checkIfPreviouslyInferred(GKInstance reactionInst, String attribute, List<GKInstance> previouslyInferredInstances) throws Exception
	{
		for (GKInstance attributeInst : (Collection<GKInstance>) reactionInst.getAttributeValuesList(attribute))
		{
			GKInstance reactionSpeciesInst = (GKInstance) attributeInst.getAttributeValue(species);
			if (reactionSpeciesInst.getDBID() == speciesInst.getDBID() && attributeInst.getAttributeValue(isChimeric) == null)
			{
				previouslyInferredInstances.add(attributeInst);
			}
		}
		return previouslyInferredInstances;
	}

	private static void outputReport(String species) throws IOException
	{
		int eligibleCount = SkipInstanceChecker.getEligibleCount();
		int inferredCount = ReactionInferrer.getInferredCount();
		float percentInferred = (float) 100*inferredCount/eligibleCount;
		// Create file if it doesn't exist
		String reportFilename = "report_ortho_inference_test_reactome_" + releaseVersion + ".txt";
		logger.info("Updating " + reportFilename);
		if (!Files.exists(Paths.get(reportFilename))) {
			createNewFile(reportFilename);
			String reportHeader = "## Number of inferred reactions by species for Reactome Release " + releaseVersion;
			Files.write(Paths.get(reportFilename), reportHeader.getBytes(), StandardOpenOption.APPEND);
		}
		String results = "hsap to " + species + ":\tInferred " + inferredCount + " out of " + eligibleCount + " eligible reactions (" + String.format("%.2f", percentInferred) + "%)\n";
		Files.write(Paths.get(reportFilename), results.getBytes(), StandardOpenOption.APPEND);
	}
	
	// Statically store the adaptor variable in each class
	private static void setDbAdaptors(MySQLAdaptor dbAdaptor)
	{
		ReactionInferrer.setAdaptor(dbAdaptor);
		SkipInstanceChecker.setAdaptor(dbAdaptor);
		InstanceUtilities.setAdaptor(dbAdaptor);
		OrthologousEntityGenerator.setAdaptor(dbAdaptor);
		EWASInferrer.setAdaptor(dbAdaptor);
		PathwaysInferrer.setAdaptor(dbAdaptor);
		
	}

	// Read the species-specific orthopair 'mapping' file, and create a HashMap with the contents
	private static Map<String, String[]> readHomologueMappingFile(String toSpecies, String fromSpecies, String pathToOrthopairs) throws IOException
	{
		String orthopairsFileName = fromSpecies + "_" + toSpecies + "_mapping.txt";
		String orthopairsFilePath = pathToOrthopairs + orthopairsFileName;
		logger.info("Reading in " + orthopairsFilePath);
		FileReader fr = new FileReader(orthopairsFilePath);
		BufferedReader br = new BufferedReader(fr);

		Map<String, String[]> homologueMappings = new HashMap<>();
		String currentLine;
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
	private static void createAndSetSpeciesInstance(String toSpeciesLong) throws Exception
	{
		SchemaClass referenceDb = dbAdaptor.getSchema().getClassByName(Species);
		speciesInst = new GKInstance(referenceDb);
		speciesInst.setDbAdaptor(dbAdaptor);
		speciesInst.addAttributeValue(created, instanceEditInst);
		speciesInst.addAttributeValue(name, toSpeciesLong);
		speciesInst.addAttributeValue(_displayName, toSpeciesLong);
		speciesInst = InstanceUtilities.checkForIdenticalInstances(speciesInst, null);
		logger.info("Using species instance: " + speciesInst);
		OrthologousEntityGenerator.setSpeciesInstance(speciesInst);
		EWASInferrer.setSpeciesInstance(speciesInst);
		InstanceUtilities.setSpeciesInstance(speciesInst);
	}
	// Create and set static Summation instance
	private static void setSummationInstance() throws Exception
	{
		GKInstance summationInst = new GKInstance(dbAdaptor.getSchema().getClassByName(Summation));
		summationInst.setDbAdaptor(dbAdaptor);
		summationInst.addAttributeValue(created, instanceEditInst);
		summationInst.addAttributeValue(text, summationText);
		summationInst.addAttributeValue(_displayName, summationText);
		summationInst = InstanceUtilities.checkForIdenticalInstances(summationInst, null);
		ReactionInferrer.setSummationInstance(summationInst);
		PathwaysInferrer.setSummationInstance(summationInst);
	}
	// Create and set static EvidenceType instance
	private static void setEvidenceTypeInstance() throws Exception
	{
		GKInstance evidenceTypeInst = new GKInstance(dbAdaptor.getSchema().getClassByName(EvidenceType));
		evidenceTypeInst.setDbAdaptor(dbAdaptor);
		evidenceTypeInst.addAttributeValue(created, instanceEditInst);
		String evidenceTypeText = "inferred by electronic annotation";
		evidenceTypeInst.addAttributeValue(name, evidenceTypeText);
		evidenceTypeInst.addAttributeValue(name, "IEA");
		evidenceTypeInst.addAttributeValue(_displayName, evidenceTypeText);
		evidenceTypeInst = InstanceUtilities.checkForIdenticalInstances(evidenceTypeInst, null);
		ReactionInferrer.setEvidenceTypeInstance(evidenceTypeInst);
		PathwaysInferrer.setEvidenceTypeInstance(evidenceTypeInst);
	}
	
	private static void setInstanceEdits(int personId) throws Exception
	{
		instanceEditInst = InstanceEditUtils.createInstanceEdit(dbAdaptor, personId, "org.reactome.orthoinference");
		logger.info("Instance edit: " + instanceEditInst);
		InstanceUtilities.setInstanceEdit(instanceEditInst);
		OrthologousEntityGenerator.setInstanceEdit(instanceEditInst);
		EWASInferrer.setInstanceEdit(instanceEditInst);
		PathwaysInferrer.setInstanceEdit(instanceEditInst);
	}
}
