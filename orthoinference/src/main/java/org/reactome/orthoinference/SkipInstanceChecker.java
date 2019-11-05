package org.reactome.orthoinference;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.ClassAttributeFollowingInstruction;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import static org.gk.model.ReactomeJavaConstants.*;
import org.gk.persistence.MySQLAdaptor;

public class SkipInstanceChecker {
	
	private static final Logger logger = LogManager.getLogger();
	private static MySQLAdaptor dba;
	private static Set<String> skipList = new HashSet<>();
	private static Map<String, String[]> homologueMappings = new HashMap<>();
	private static String eligibleFilehandle;
	private static Integer eligibleCount = 0;
	
	// Skiplist was traditionally provided in a file, but since it's currently just 3 instances, I've just hard-coded them here.
	public static void getSkipList(String skipListFilename) throws NumberFormatException, Exception
	{
		String[] pathwayIdsToSkip = {"162906","168254","977225"};
		for (String pathwayId : pathwayIdsToSkip) 
		{
			GKInstance pathwayInst = dba.fetchInstance(Long.valueOf(pathwayId));
			if (pathwayInst != null)
			{
				// Finds all ReactionLikeEvents associated with the skiplists Pathway and hasEvent attributes, and adds them to skiplist.
				List<ClassAttributeFollowingInstruction> classesToFollow = new ArrayList<>();
				classesToFollow.add(new ClassAttributeFollowingInstruction(Pathway, new String[]{hasEvent}, new String[]{}));
				String[] outClasses = new String[] {ReactionlikeEvent};
				@SuppressWarnings("unchecked")
				Collection<GKInstance> followedInstances = InstanceUtilities.followInstanceAttributes(pathwayInst, classesToFollow, outClasses);
				
				for (GKInstance entityInst : followedInstances)
				{
					skipList.add(entityInst.getDBID().toString());
				}
			}
		}
		// Generates skiplist
		String skipListFilePath = "src/main/resources/" + skipListFilename; 
		FileReader fr = new FileReader(skipListFilePath);
		BufferedReader br = new BufferedReader(fr);
		String currentLine;
		while ((currentLine = br.readLine()) != null)
		{
			skipList.add(currentLine.trim());
		}
		br.close();
		fr.close();
	}
	// Skip orthoinference of this instance if:
	public static boolean checkIfInstanceShouldBeSkipped(GKInstance reactionInst) throws Exception
	{
		// it is found in skiplist array
		if (skipList.contains(reactionInst.getDBID().toString()))
		{
			logger.info(reactionInst + " is in skipList -- skipping");
			return true;
		}
		// it is chimeric
		if (reactionInst.getAttributeValue(isChimeric) != null)
		{
			if ((boolean) reactionInst.getAttributeValue(isChimeric))
			{
				logger.info(reactionInst + " is chimeric -- skipping");
				return true;
			}
		}
		// it has related species
		if (reactionInst.getAttributeValue("relatedSpecies") != null)
		{
			logger.info(reactionInst + " has related species -- skipping");
			return true;
		}
		// it is a disease reaction
		if (reactionInst.getAttributeValue(disease) != null)
		{
			logger.info(reactionInst + " is a disease reaction -- skipping");
			return true;
		}
		// it is manually inferred
		if (reactionInst.getAttributeValue(inferredFrom) != null)
		{
			logger.info(reactionInst + " is manually inferred -- skipping");
			return true;
		}
		// it contains multiple species
		Collection<GKInstance> speciesInstances = checkIfEntitiesContainMultipleSpecies(reactionInst);
		if (speciesInstances.size() > 1)
		{
			logger.info(reactionInst + " has multiple species -- skipping");
			return true;
		}

		// This function finds the total number of distinct proteins associated with an instance, as well as the number that can be inferred.
		// Total proteins are stored in reactionProteinCounts[0], inferrable proteins in [1], and the maximum number of homologues for any entity involved in index [2].
		// Reactions with no proteins/EWAS (Total = 0) are not inferred.
		List<Integer> reactionProteinCounts = ProteinCountUtility.getDistinctProteinCounts(reactionInst);
		int reactionTotalProteinCounts = reactionProteinCounts.get(0);
		if (reactionTotalProteinCounts > 0) {
			// Since we want to keep the eligibility counts the same for posterity, this is where Reaction eligibility will be determined, instead of in ReactionInferrer. (October 2019)
			logger.info("Total protein count for RlE: " + reactionTotalProteinCounts);
			eligibleCount++;
			String eligibleEventName = reactionInst.getAttributeValue(DB_ID).toString() + "\t" + reactionInst.getDisplayName() + "\n";
			Files.write(Paths.get(eligibleFilehandle), eligibleEventName.getBytes(), StandardOpenOption.APPEND);
			// Checks that ReactionlikeEvents will be fully inferrable before attempting inference
			if (!reactionComponentsAreInferrable(reactionInst)) {
				return true;
			}
		} else {
				logger.info("No distinct proteins found in instance -- terminating inference for " + reactionInst);
				return true;
			}
		return false;
	}

	/**
	 * 	Each input, output, and catalyst is screened to verify that the Reaction will be inferred.
	 * 	This prevents the majority of orphan PEs that were being created during orthoinference.
	 * 	Only some EntitySets are still orphaned due to the complexity behind attempting to screen them
	 * @param reactionInst -- GKInstance that will be screened
	 * @return -- Boolean is returned that indicates if Reaction is fully inferrable or not
	 * @throws Exception
	 */
	private static boolean reactionComponentsAreInferrable(GKInstance reactionInst) throws Exception {
		// First gather all inputs, outputs and the PEs in catalyst activities
		// Inputs/Outputs/CatalystPEs need to be stored in seperate collections. At time of writing, having it all stored in
		// the same collection causes outputs to be inferred in both inputs and outputs during the actual inference -- not ideal
		Collection<GKInstance> reactionInputs = reactionInst.getAttributeValuesList(input);
		Collection<GKInstance> reactionOutputs = reactionInst.getAttributeValuesList(output);
		Collection<GKInstance> reactionCatalystPEs = new ArrayList<>();
		Collection<GKInstance> reactionCatalysts = reactionInst.getAttributeValuesList(catalystActivity);
		for (GKInstance reactionCatalyst : reactionCatalysts) {
			GKInstance catalystPE = (GKInstance) reactionCatalyst.getAttributeValue(physicalEntity);
			if (catalystPE != null) {
				reactionCatalystPEs.add(catalystPE);
			}
		}
		// Screen inputs
		for (GKInstance reactionInput : reactionInputs) {
			if (!componentIsInferrable(reactionInput)) {
				return false;
			}
		}
		// Screen outputs
		for (GKInstance reactionOutput : reactionOutputs) {
			if (!componentIsInferrable(reactionOutput)) {
				return false;
			}
		}
		// Screen catalyst PhysicalEntities
		for (GKInstance reactionCatalystPE : reactionCatalystPEs) {
			if (!componentIsInferrable(reactionCatalystPE)) {
				return false;
			}
		}
		return true;
	}

	// This looks a lot like the code structure found in OrthologousEntityGenerator, just without the actual inference code or override functionality
	// This will screen all instance types except for some EntitySets, which are complex to screen ahead of time.
	private static boolean componentIsInferrable(GKInstance reactionComponent) throws Exception {
		// This block doesn't do anything aside from prevent non-species-containing instances from going through other screening.
		// During actual inference, a non-species-containing instance would be returned without any inference.
		if (!SpeciesCheckUtility.checkForSpeciesAttribute(reactionComponent)) {
//				return true;
		} else if (reactionComponent.getSchemClass().isa(GenomeEncodedEntity))
		{
			if (reactionComponent.getSchemClass().toString().contains(EntityWithAccessionedSequence)) {
				String referenceEntityId = ((GKInstance) reactionComponent.getAttributeValue(referenceEntity)).getAttributeValue(identifier).toString();
				if (homologueMappings.get(referenceEntityId) == null) {
					return false;
				}
			} else {
				return false;
			}
		} else if (reactionComponent.getSchemClass().isa(Complex) || reactionComponent.getSchemClass().isa(Polymer) || reactionComponent.getSchemClass().isa(EntitySet)) {
			List<Integer> complexProteinCounts = ProteinCountUtility.getDistinctProteinCounts(reactionComponent);
			int totalProteinCounts = complexProteinCounts.get(0);
			int inferrableProteinCounts = complexProteinCounts.get(1);
			if (reactionComponent.getSchemClass().isa(Complex) || reactionComponent.getSchemClass().isa(Polymer)) {
				int percent = 0;
				if (totalProteinCounts > 0) {
					percent = (inferrableProteinCounts * 100) / totalProteinCounts;
				}
				if (percent < 75) {
					logger.info("Complex/Polymer protein count is below 75% threshold (" + percent + "%) -- terminating inference");
					return false;
				}
			} else if (totalProteinCounts > 0 && inferrableProteinCounts == 0) {
				logger.info("No distinct proteins found in EntitySet -- terminating inference");
				return false;
			}
		}
		return true;
	}

	// Goes through all input/output/catalystActivity/regulatedBy attribute instances, and captures all species associates with them. Returns a collection of species instances.
	@SuppressWarnings("unchecked")
	private static Collection<GKInstance> checkIfEntitiesContainMultipleSpecies(GKInstance reactionInst) throws Exception
	{
		List<GKInstance> physicalEntityInstances = new ArrayList<>();
		physicalEntityInstances.addAll(reactionInst.getAttributeValuesList(input));
		physicalEntityInstances.addAll(reactionInst.getAttributeValuesList(output));
		for (GKInstance catalystActivityInst : (Collection<GKInstance>) reactionInst.getAttributeValuesList(catalystActivity))
		{
			physicalEntityInstances.addAll(catalystActivityInst.getAttributeValuesList(physicalEntity));
		}
		List<GKInstance> regulatedByInstances = (ArrayList<GKInstance>) reactionInst.getAttributeValuesList("regulatedBy");

		if (regulatedByInstances != null) 
		{
			for (GKInstance regulatedByInst : regulatedByInstances)
			{
				for (GKInstance regulatorInst : (Collection<GKInstance>) regulatedByInst.getAttributeValuesList(regulator))
				{
					if (regulatorInst.getSchemClass().isa(PhysicalEntity))
					{
						physicalEntityInstances.add(regulatorInst);
					}
				}
			}
		}
		Map<String, GKInstance> physicalEntityHash = new HashMap<>();
		// Remove duplicates using HashMap
		for (GKInstance physicalEntityInst : physicalEntityInstances)
		{
			physicalEntityHash.put(physicalEntityInst.getDBID().toString(), physicalEntityInst);
		}
		Map<String, GKInstance> physicalEntitiesFinal = new HashMap<>();
		for (GKInstance physicalEntityInst : physicalEntityHash.values())
		{
			physicalEntitiesFinal.put(physicalEntityInst.getDBID().toString(), physicalEntityInst);
			Collection<GKInstance> allConstituentInstances = recursePhysicalEntityConstituentInstances(physicalEntityInst);
			if (allConstituentInstances != null)
			{
				for (GKInstance constituentInst : allConstituentInstances)
				{
					physicalEntitiesFinal.put(constituentInst.getDBID().toString(), constituentInst);
				}
			}
		}
		Map<String, GKInstance> speciesHash = new HashMap<>();
		for (GKInstance physicalEntityInst : physicalEntitiesFinal.values())
		{
			if (physicalEntityInst.getSchemClass().isValidAttribute(species))
			{
				for (GKInstance speciesInst : (Collection<GKInstance>) physicalEntityInst.getAttributeValuesList(species))
				{
					speciesHash.put(speciesInst.getDBID().toString(), speciesInst);
				}
			}
		}
		return speciesHash.values();
	}
	
	// Looks at referrals of the constituent instances for the species attribute as well
	// The term 'constituent' is used as a catch-all for instances under the hasMember/hasComponent/repeatedUnit attributes
	@SuppressWarnings("unchecked")
	private static Collection<GKInstance> recursePhysicalEntityConstituentInstances(GKInstance physicalEntity) throws Exception
	{
		Map<String, GKInstance> constituentInstances = new HashMap<>();
		if (physicalEntity.getSchemClass().isValidAttribute(hasMember))
		{
			for (GKInstance memberInst : (Collection<GKInstance>) physicalEntity.getAttributeValuesList(hasMember))
			{
				constituentInstances.put(memberInst.getDBID().toString(), memberInst);
			}
		}
		if (physicalEntity.getSchemClass().isValidAttribute(hasComponent))
		{
			for (GKInstance componentInst : (Collection<GKInstance>) physicalEntity.getAttributeValuesList(hasComponent))
			{
				constituentInstances.put(componentInst.getDBID().toString(), componentInst);
			}
		}
		if (physicalEntity.getSchemClass().isValidAttribute(repeatedUnit))
		{
			for (GKInstance repeatedUnitInst : (Collection<GKInstance>) physicalEntity.getAttributeValuesList(repeatedUnit))
			{
				constituentInstances.put(repeatedUnitInst.getDBID().toString(), repeatedUnitInst);
			}
		}
		if (constituentInstances.size() > 0)
		{
			Map<String, GKInstance> finalConstituentInstancesMap = new HashMap<>();
			for (GKInstance constituentInst : constituentInstances.values())
			{	
				finalConstituentInstancesMap.put(constituentInst.getDBID().toString(), constituentInst);
				if (constituentInst.getSchemClass().isa(EntitySet) || constituentInst.getSchemClass().isa(Complex) || constituentInst.getSchemClass().isa(Polymer))
				{
					Collection<GKInstance> recursedConstituentInstances = recursePhysicalEntityConstituentInstances(constituentInst);
					if (recursedConstituentInstances != null)
					{
						for (GKInstance recursedConstituentInst : recursedConstituentInstances)
						{
							finalConstituentInstancesMap.put(recursedConstituentInst.getDBID().toString(), recursedConstituentInst);
						}
					}
				} else {
					continue;
				}
			}
			return finalConstituentInstancesMap.values();
		}
		return null;
	}
	
	public static void setAdaptor(MySQLAdaptor dbAdaptor)
	{
		dba = dbAdaptor;
	}

	public static void setHomologueMappingFile(Map<String, String[]> homologueMappingsCopy) { homologueMappings = homologueMappingsCopy; }

	public static void setEligibleFilename(String eligibleFilename)
	{
		eligibleFilehandle = eligibleFilename;
	}

	public static int getEligibleCount()
	{
		return eligibleCount;
	}
}
