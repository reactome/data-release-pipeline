package org.reactome.orthoinference;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.gk.schema.SchemaClass;

public class InferEWAS {

	private static MySQLAdaptor dba;
	static boolean refDb = false;
	private static String altRefDBId;
	private static GKInstance instanceEdit;
	private static GKInstance ensgDbInst;
	private static GKInstance enspDbInst;
	private static GKInstance alternateDbInst;
	private static GKInstance uniprotDbInst;
	private static GKInstance speciesInst;
	private static HashMap<String, String[]> homologueMappings = new HashMap<String,String[]>();
	private static HashMap<String, ArrayList<String>> ensgMappings = new HashMap<String,ArrayList<String>>();
	private static HashMap<String, GKInstance> seenRGP = new HashMap<String,GKInstance>();
	private static HashMap<String,GKInstance> ewasIdenticals = new HashMap<String,GKInstance>();
	private static HashMap<String,GKInstance> residueIdenticals = new HashMap<String,GKInstance>();

    // Creates an array of inferred EWAS instances from the homologue mappings file (hsap_species_mapping.txt).
	@SuppressWarnings("unchecked")
	public static ArrayList<GKInstance> ewasInferrer(GKInstance ewasInst) throws InvalidAttributeException, Exception
	{
		ArrayList<GKInstance> infEWASInstances = new ArrayList<GKInstance>();
		String referenceEntityId = ((GKInstance) ewasInst.getAttributeValue(ReactomeJavaConstants.referenceEntity)).getAttributeValue(ReactomeJavaConstants.identifier).toString();
		if (homologueMappings.get(referenceEntityId) != null)
			{
				// Iterate through the array of homologue mappings, attempting to infer EWAS instances for each.
				for (Object homologue : homologueMappings.get(referenceEntityId))
				{
					String[] splitHomologue = homologue.toString().split(":");
					String homologueSource = splitHomologue[0];
					String homologueId = splitHomologue[1];
					
					GKInstance infReferenceGeneProduct = null;
					if (seenRGP.get(homologueId) == null)
					{
						infReferenceGeneProduct = InstanceUtilities.newInferredGKInstance((GKInstance) ewasInst.getAttributeValue(ReactomeJavaConstants.referenceEntity));
						infReferenceGeneProduct.addAttributeValue(ReactomeJavaConstants.identifier, homologueId);
						// Reference DB can differ between homologue mappings, but can be differentiated by the 'homologueSource' found in each mapping.
						GKInstance referenceDb = null;
						if (homologueSource.equals("ENSP"))
						{
							referenceDb = enspDbInst;
						} else {
							referenceDb = uniprotDbInst;
						}
						infReferenceGeneProduct.addAttributeValue(ReactomeJavaConstants.referenceDatabase,  referenceDb);
						
						//// Creates ReferenceDNASequence instance from ReferenceEntity
						ArrayList<GKInstance> inferredReferenceDNAInstances = InferEWAS.createReferenceDNASequence(homologueId);
						infReferenceGeneProduct.addAttributeValue(ReactomeJavaConstants.referenceGene, inferredReferenceDNAInstances);
						
						infReferenceGeneProduct.addAttributeValue(ReactomeJavaConstants.species, speciesInst);
						infReferenceGeneProduct.setAttributeValue(ReactomeJavaConstants._displayName, "UniProt:" + homologueId);
						infReferenceGeneProduct = InstanceUtilities.checkForIdenticalInstances(infReferenceGeneProduct);
						seenRGP.put(homologueId, infReferenceGeneProduct);
					} else {
						infReferenceGeneProduct = seenRGP.get(homologueId);
					}
					// Creating inferred EWAS
					GKInstance infEWAS = InstanceUtilities.newInferredGKInstance(ewasInst);
					infEWAS.addAttributeValue(ReactomeJavaConstants.referenceEntity, infReferenceGeneProduct);
					// Method for adding start/end coordinates. It is convoluted due to a quirk with assigning the name differently based on coordinate value (see infer_events.pl lines 1190-1192). 
					// The name of the entity needs to be at the front of the 'name' array if the coordinate is over 1, and rearranging arrays in Java for this was a bit tricky.
					for (Object startCoordinate : ewasInst.getAttributeValuesList(ReactomeJavaConstants.startCoordinate))
					{
						infEWAS.addAttributeValue(ReactomeJavaConstants.startCoordinate, startCoordinate);
					}
					for (Object endCoordinate : ewasInst.getAttributeValuesList(ReactomeJavaConstants.endCoordinate))
					{
						infEWAS.addAttributeValue(ReactomeJavaConstants.endCoordinate, endCoordinate);
					}
					Object startCoordObj = infEWAS.getAttributeValue(ReactomeJavaConstants.startCoordinate);
					Object endCoordObj = infEWAS.getAttributeValue(ReactomeJavaConstants.endCoordinate);
					Integer startCoord = 0;
					Integer endCoord = 0;
					if (startCoordObj != null)
					{
						startCoord = Integer.valueOf(infEWAS.getAttributeValue(ReactomeJavaConstants.startCoordinate).toString());
					}
					if (endCoordObj != null)
					{
						endCoord = Integer.valueOf(infEWAS.getAttributeValue(ReactomeJavaConstants.endCoordinate).toString());
					}
					if (startCoord > 1 || endCoord > 1) {
						ArrayList<String> infAttributeInstNames = (ArrayList<String>) ((GKInstance) ewasInst).getAttributeValuesList(ReactomeJavaConstants.name);
						infEWAS.addAttributeValue(ReactomeJavaConstants.name, infAttributeInstNames.get(0));
						infEWAS.addAttributeValue(ReactomeJavaConstants.name, homologueId);
					} else {
						infEWAS.addAttributeValue(ReactomeJavaConstants.name, homologueId);
					}
					String displayName = (String) infEWAS.getAttributeValue(ReactomeJavaConstants.name) + " [" +((GKInstance) ewasInst.getAttributeValue(ReactomeJavaConstants.compartment)).getDisplayName() + "]";
					infEWAS.setAttributeValue(ReactomeJavaConstants._displayName, displayName);

					// Infer residue modifications. This was another step where the name of an EWAS can change.
					// For this, it is based on the existence of the string 'phospho' in the name of the psiMod attribute. 
					// If true, 'phospho-' is prepended to the EWAS' name attribute.
					ArrayList<GKInstance> infModifiedResidues = new ArrayList<GKInstance>();
					boolean phosFlag = true;
					for (GKInstance modifiedResidue : (Collection<GKInstance>) ewasInst.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue))
					{
						String infModifiedResidueDisplayName = "";
						GKInstance infModifiedResidue = InstanceUtilities.newInferredGKInstance(modifiedResidue);
						infModifiedResidue.addAttributeValue(ReactomeJavaConstants.referenceSequence, infReferenceGeneProduct);
						infModifiedResidueDisplayName += infReferenceGeneProduct.getDisplayName();
						for (Object coordinate : modifiedResidue.getAttributeValuesList(ReactomeJavaConstants.coordinate))
						{
							infModifiedResidue.addAttributeValue(ReactomeJavaConstants.coordinate, coordinate);
						}
						if (infModifiedResidue.getSchemClass().isValidAttribute(ReactomeJavaConstants.modification))
						{
							for (GKInstance modified : (Collection<GKInstance>) modifiedResidue.getAttributeValuesList(ReactomeJavaConstants.modification))
							{
								infModifiedResidue.addAttributeValue(ReactomeJavaConstants.modification, modified);
							}
							if (infModifiedResidue.getAttributeValue("modification") != null) {
								infModifiedResidueDisplayName += " " + ((GKInstance) infModifiedResidue.getAttributeValue(ReactomeJavaConstants.modification)).getDisplayName();
							}
						}
						// Update name depending on the presence of 'phospho' in the Psimod's name attribute
						GKInstance psiMod = (GKInstance) modifiedResidue.getAttributeValue(ReactomeJavaConstants.psiMod);
						if (phosFlag && psiMod.getAttributeValue(ReactomeJavaConstants.name).toString().contains("phospho"))
						{
							String phosphoName = "phospho-" + infEWAS.getAttributeValue(ReactomeJavaConstants.name);
							ArrayList<GKInstance> nameValues = (ArrayList<GKInstance>) infEWAS.getAttributeValuesList(ReactomeJavaConstants.name);
							nameValues.remove(0);
							infEWAS.setAttributeValue(ReactomeJavaConstants.name, phosphoName);
							// In the Perl version, this code block modifies the 'name' attribute to include 'phosopho-', but in the process it drops the other names contained. I believe this is unintentional.
							// This would mean attributes without the 'phospho- ' addition would retain their array of names, while attributes containing 'phospho-' would only contain a single name attribute. 
							// I've assumed this is incorrect for the rewrite -- Instances that modify the name attribute to prepend 'phospho-' retain their name array. (Justin Cook 2018)
							infEWAS.addAttributeValue(ReactomeJavaConstants.name, nameValues); 
							String phosphoDisplayName = phosphoName + " [" +((GKInstance) ewasInst.getAttributeValue(ReactomeJavaConstants.compartment)).getDisplayName() + "]";
							infEWAS.setAttributeValue(ReactomeJavaConstants._displayName, phosphoDisplayName);
							// This flag ensures the 'phospho-' is only prepended once.
							phosFlag = false;
						}
						for (GKInstance psiModInst : (Collection<GKInstance>) modifiedResidue.getAttributeValuesList(ReactomeJavaConstants.psiMod))
						{
							infModifiedResidue.addAttributeValue(ReactomeJavaConstants.psiMod, psiModInst);
						}
						if (infModifiedResidue.getAttributeValue(ReactomeJavaConstants.psiMod) != null) {
							infModifiedResidueDisplayName += " " + ((GKInstance) infModifiedResidue.getAttributeValue(ReactomeJavaConstants.psiMod)).getDisplayName();
						}
						infModifiedResidue.setAttributeValue(ReactomeJavaConstants._displayName, modifiedResidue.getAttributeValue(ReactomeJavaConstants._displayName));
						// Update name to reflect that coordinate values are taken from humans. This takes place after cache retrieval, since the name from DB won't contain updated name.
						if (modifiedResidue.getAttributeValue(ReactomeJavaConstants.coordinate) != null)
						{
							// TODO: Abstract this out so that its 'in fromSpecies' if code ever wants to infer from a different species than Humans.
							String newDisplayName = modifiedResidue.getAttributeValue(ReactomeJavaConstants._displayName).toString() + " (in Homo sapiens)";
							infModifiedResidue.setAttributeValue(ReactomeJavaConstants._displayName, newDisplayName);
							
						} else {
							if (infModifiedResidue.getSchemClass().isa(ReactomeJavaConstants.InterChainCrosslinkedResidue)) {
								infModifiedResidue.setDisplayName(infModifiedResidueDisplayName);
							}
						}
						// Database-checker gave errors related to missing 'secondReferenceSequence' and 'equivalentTo' attributes in InterChainCrosslinkedResidues
						// This was because they were never populated. This block is the fix.
						if (infModifiedResidue.getSchemClass().isa(ReactomeJavaConstants.InterChainCrosslinkedResidue)) {
							if (modifiedResidue.getAttributeValue(ReactomeJavaConstants.secondReferenceSequence) != null) {
								for (Object secondRefSequence : modifiedResidue.getAttributeValuesList(ReactomeJavaConstants.secondReferenceSequence)) {
									infModifiedResidue.addAttributeValue(ReactomeJavaConstants.secondReferenceSequence, secondRefSequence);
								}
							}
							if (modifiedResidue.getAttributeValue("equivalentTo") != null) {
								for (Object equivalentToInst : modifiedResidue.getAttributeValuesList("equivalentTo")) {
									infModifiedResidue.addAttributeValue("equivalentTo", equivalentToInst);
								}
							}
						}
						// Caching based on an instance's defining attributes. This reduces the number of 'checkForIdenticalInstance' calls, which slows things.
						String cacheKey = InstanceUtilities.getCacheKey((GKSchemaClass) infModifiedResidue.getSchemClass(), infModifiedResidue);
						if (residueIdenticals.get(cacheKey) != null)
						{
							infModifiedResidue = residueIdenticals.get(cacheKey);
						} else {
							infModifiedResidue = InstanceUtilities.checkForIdenticalInstances(infModifiedResidue);
							residueIdenticals.put(cacheKey, infModifiedResidue);
						}
						infModifiedResidues.add((GKInstance) infModifiedResidue);
					}
					infEWAS.addAttributeValue(ReactomeJavaConstants.hasModifiedResidue, infModifiedResidues);
					// Caching based on an instance's defining attributes. This reduces the number of 'checkForIdenticalInstance' calls, which slows things.
					String cacheKey = InstanceUtilities.getCacheKey((GKSchemaClass) infEWAS.getSchemClass(), infEWAS);
					if (ewasIdenticals.get(cacheKey) != null)
					{
						infEWAS = ewasIdenticals.get(cacheKey);
					} else {
						infEWAS = InstanceUtilities.checkForIdenticalInstances(infEWAS);
						ewasIdenticals.put(cacheKey, infEWAS);
					}

					infEWAS = InstanceUtilities.addAttributeValueIfNeccesary(infEWAS, ewasInst, ReactomeJavaConstants.inferredFrom);
					dba.updateInstanceAttribute(infEWAS, ReactomeJavaConstants.inferredFrom);
					ewasInst = InstanceUtilities.addAttributeValueIfNeccesary(ewasInst, infEWAS, ReactomeJavaConstants.inferredTo);
					dba.updateInstanceAttribute(ewasInst, ReactomeJavaConstants.inferredTo);

					infEWASInstances.add((GKInstance) infEWAS);
				}
			}
		return infEWASInstances;
	}

    // Creates ReferenceGeneSequence instance based on ENSG identifier mapped to protein. Creates an instance for the primary database and an alternate, if it exists.
	public static ArrayList<GKInstance> createReferenceDNASequence(String homologueId) throws InvalidAttributeException, InvalidAttributeValueException, Exception
	{
		ArrayList<GKInstance> referenceDNAInstances = new ArrayList<GKInstance>();
		ArrayList<String> ensgs = ensgMappings.get(homologueId);
		for (Object ensg : ensgs)
		{
			SchemaClass referenceDNAClass = dba.getSchema().getClassByName(ReactomeJavaConstants.ReferenceDNASequence);
			GKInstance referenceDNAInst = new GKInstance(referenceDNAClass);
			referenceDNAInst.setDbAdaptor(dba);
			referenceDNAInst.addAttributeValue(ReactomeJavaConstants.created, instanceEdit);
			referenceDNAInst.addAttributeValue(ReactomeJavaConstants.identifier, ensg);
			referenceDNAInst.addAttributeValue(ReactomeJavaConstants.referenceDatabase, ensgDbInst);
			referenceDNAInst.addAttributeValue(ReactomeJavaConstants.species, speciesInst);
			referenceDNAInst.setAttributeValue(ReactomeJavaConstants._displayName, "ENSEMBL:" + ensg);
			referenceDNAInst = InstanceUtilities.checkForIdenticalInstances(referenceDNAInst);
			referenceDNAInstances.add(referenceDNAInst);
			if (refDb)
			{
				GKInstance alternateRefDNAInst = new GKInstance(referenceDNAClass);
				alternateRefDNAInst.setDbAdaptor(dba);
				String altDbIdentifier = (String) ensg;
				if (altRefDBId != null) {
					altDbIdentifier = altDbIdentifier.replaceAll(altRefDBId, "");
				}
				alternateRefDNAInst.addAttributeValue(ReactomeJavaConstants.created, instanceEdit);
				alternateRefDNAInst.addAttributeValue(ReactomeJavaConstants.identifier, altDbIdentifier);
				alternateRefDNAInst.addAttributeValue(ReactomeJavaConstants.referenceDatabase, alternateDbInst);
				alternateRefDNAInst.addAttributeValue(ReactomeJavaConstants.species, speciesInst);
				alternateRefDNAInst.setAttributeValue(ReactomeJavaConstants._displayName, alternateDbInst.getAttributeValue(ReactomeJavaConstants.name) + ":" + ensg);
				alternateRefDNAInst = InstanceUtilities.checkForIdenticalInstances(alternateRefDNAInst);
				referenceDNAInstances.add(alternateRefDNAInst);
			}
		}
		return referenceDNAInstances;
	}
	
    // These are setup functions called at the beginning of the 'inferEvent' script 
	
	public static void setAdaptor(MySQLAdaptor dbAdaptor)
	{
		InferEWAS.dba = dbAdaptor;
	}
	// Sets the HashMap of species-specific homologue-identifier mappings
	public static void setHomologueMappingFile(HashMap<String, String[]> homologueMappingsCopy) throws IOException
	{
		homologueMappings = homologueMappingsCopy;
	}
	
	// Read the species-specific ENSG gene-protein mappings, and create a Hashmap with the contents
	public static void readENSGMappingFile(String toSpecies) throws IOException
	{
		String mappingFileName = toSpecies + "_gene_protein_mapping.txt";
		String mappingFilePath = "src/main/resources/orthopairs/" + mappingFileName;
		FileReader fr = new FileReader(mappingFilePath);
		BufferedReader br = new BufferedReader(fr);

		String currentLine;
		while ((currentLine = br.readLine()) != null)
		{
			String[] tabSplit = currentLine.split("\t");
			String ensgKey = tabSplit[0];
			String[] spaceSplit = tabSplit[1].split(" ");
			for (String proteinId : spaceSplit) {
				String[] colonSplit = proteinId.split(":");
				if (ensgMappings.get(colonSplit[1]) == null)
				{
					ArrayList<String> singleArray = new ArrayList<String>();
					singleArray.add(ensgKey);
					ensgMappings.put(colonSplit[1], singleArray);
				} else {
					ensgMappings.get(colonSplit[1]).add(ensgKey);
				}
			}
		}
		br.close();
		fr.close();
	}
	
	// Fetches Uniprot DB instance
	@SuppressWarnings("unchecked")
	public static void createUniprotDbInst() throws Exception
	{
		 Collection<GKInstance> uniprotDbInstances = (Collection<GKInstance>) dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase, ReactomeJavaConstants.name, "=", "UniProt");
		 uniprotDbInst = uniprotDbInstances.iterator().next();
	}
	
	// Creates instance pertaining to the species Ensembl Protein DB
	public static void createEnsemblProteinDbInst(String toSpeciesLong, String toSpeciesReferenceDbUrl, String toSpeciesEnspAccessUrl) throws InvalidAttributeException, InvalidAttributeValueException, Exception
	{
		String enspSpeciesDb = "ENSEMBL_" + toSpeciesLong + "_PROTEIN";
		SchemaClass referenceDb = dba.getSchema().getClassByName(ReactomeJavaConstants.ReferenceDatabase);
		enspDbInst = new GKInstance(referenceDb);
		enspDbInst.setDbAdaptor(dba);
		enspDbInst.addAttributeValue(ReactomeJavaConstants.created, instanceEdit);
		enspDbInst.addAttributeValue(ReactomeJavaConstants.name, "Ensembl"); 
		enspDbInst.addAttributeValue(ReactomeJavaConstants.name, enspSpeciesDb);
		enspDbInst.addAttributeValue(ReactomeJavaConstants.url, toSpeciesReferenceDbUrl);
		enspDbInst.addAttributeValue(ReactomeJavaConstants.accessUrl, toSpeciesEnspAccessUrl);
		enspDbInst.setAttributeValue(ReactomeJavaConstants._displayName, "Ensembl");
//		enspDbInst = InstanceUtilities.checkForIdenticalInstances(enspDbInst);
		dba.storeInstance(enspDbInst);
	}
	
	// Creates instance pertaining to the species Ensembl Gene DB
	public static void createEnsemblGeneDBInst(String toSpeciesLong, String toSpeciesReferenceDbUrl, String toSpeciesEnsgAccessUrl) throws InvalidAttributeException, InvalidAttributeValueException, Exception
	{
		String ensgSpeciesDb = "ENSEMBL_" + toSpeciesLong + "_GENE";
		SchemaClass referenceDb = dba.getSchema().getClassByName(ReactomeJavaConstants.ReferenceDatabase);
		ensgDbInst = new GKInstance(referenceDb);
		ensgDbInst.setDbAdaptor(dba);
		ensgDbInst.addAttributeValue(ReactomeJavaConstants.created, instanceEdit);
		ensgDbInst.addAttributeValue(ReactomeJavaConstants.name, "ENSEMBL"); 
		ensgDbInst.addAttributeValue(ReactomeJavaConstants.name, ensgSpeciesDb);
		ensgDbInst.addAttributeValue(ReactomeJavaConstants.url, toSpeciesReferenceDbUrl);
		ensgDbInst.addAttributeValue(ReactomeJavaConstants.accessUrl, toSpeciesEnsgAccessUrl);
		ensgDbInst.setAttributeValue(ReactomeJavaConstants._displayName, "ENSEMBL");
//		ensgDbInst = InstanceUtilities.checkForIdenticalInstances(ensgDbInst);
		dba.storeInstance(ensgDbInst);
	}
	
	// Create instance pertaining to any alternative reference DB for the species
	public static void createAlternateReferenceDBInst(String toSpeciesLong, String alternateDbName, String toSpeciesAlternateDbUrl, String toSpeciesAlternateAccessUrl) throws InvalidAttributeException, InvalidAttributeValueException, Exception
	{
		SchemaClass alternateDb = dba.getSchema().getClassByName(ReactomeJavaConstants.ReferenceDatabase);
		alternateDbInst = new GKInstance(alternateDb);
		alternateDbInst.setDbAdaptor(dba);
		alternateDbInst.addAttributeValue(ReactomeJavaConstants.created, instanceEdit);
		alternateDbInst.addAttributeValue(ReactomeJavaConstants.name, alternateDbName);
		alternateDbInst.addAttributeValue(ReactomeJavaConstants.url, toSpeciesAlternateDbUrl);
		alternateDbInst.addAttributeValue(ReactomeJavaConstants.accessUrl, toSpeciesAlternateAccessUrl);
		alternateDbInst.setAttributeValue(ReactomeJavaConstants._displayName, alternateDbName);
		alternateDbInst = InstanceUtilities.checkForIdenticalInstances(alternateDbInst);
		refDb = true;
	}
	
	public static void setAlternateDBId(String altRefDBIdCopy) {
		altRefDBId = altRefDBIdCopy;
	}
	
	public static void updateRefDb() 
	{
		refDb = false;
	}
	
	// Sets the species instance for inferEWAS to use
	public static void setSpeciesInst(GKInstance speciesInstCopy)
	{
		speciesInst = speciesInstCopy;
	}
	
	public static void resetVariables() 
	{
		homologueMappings = new HashMap<String,String[]>();
		ensgMappings = new HashMap<String,ArrayList<String>>();
		seenRGP = new HashMap<String,GKInstance>();
		ewasIdenticals = new HashMap<String,GKInstance>();
		residueIdenticals = new HashMap<String,GKInstance>();
	}
	
	public static void setInstanceEdit(GKInstance instanceEditCopy) 
	{
		instanceEdit = instanceEditCopy;
	}
}