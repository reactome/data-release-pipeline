package org.reactome.release.uniprotupdate;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

import java.util.*;

public class HumanDataProcessor extends AbstractDataProcessor
{
	public HumanDataProcessor(MySQLAdaptor adaptor, GKInstance instanceEdit)
	{
		super(adaptor, instanceEdit);
	}

	/**
	 * Process UniprotData object whose species is Human.
	 * @param referenceDNASequences - a map of ReferenceDNASequences, keyed by ENSEMBL Gene IDs
	 * @param referenceGeneProducts - a list of ReferenceGeneProducts, keyed by accession.
	 * @param genesOKWithENSEMBL - A set of Genes that are considered "OK" in ENSEMBL.
	 * @param data - the UniprotData object to process.
	 * @param geneList - a list of ENSEMBL Gene IDs for this UniprotData object.
	 * @param accession - The accession
	 * @throws InvalidAttributeException
	 * @throws Exception
	 * @throws InvalidAttributeValueException
	 */
	void processHumanData(Map<String, GKInstance> referenceDNASequences, Map<String, GKInstance> referenceGeneProducts, Set<String> genesOKWithENSEMBL, UniprotData data, List<String> geneList, /* List<GKInstance> referenceDNASequencesForThisUniprot, */ String accession) throws InvalidAttributeException, Exception, InvalidAttributeValueException
	{
		List<GKInstance> referenceDNASequencesForThisUniprot = new ArrayList<>();
		// Will need a flattened list of geneNames.
		List<String> geneNames = new ArrayList<>();
		String primaryGeneName = "";

		geneNames = data.getFlattenedGeneNames();
		if (geneNames != null && !geneNames.isEmpty())
		{
			primaryGeneName = geneNames.get(0);
		}

		// Report when there are multiple gene names.
		if (data.getGenes() != null && data.getGenes().size() > 1)
		{
			referenceDNASequenceLog.info("Accession " + data.getAccessions().toString() + " has multiple gene names: " + geneNamesListToString(data.getGenes()));
		}
		// For each ENSEMBL Gene ID that is in this chunk of Data. 
		// Note: It could happen that the same Gene ID could be repeted more than once. For example: the source XML could contain:
		//		<dbReference id="ENST00000383605" type="Ensembl">
		//			<property value="ENSP00000373100" type="protein sequence ID" />
		//			<property value="ENSG00000206505" type="gene ID" />
		//		</dbReference>
		//		<dbReference id="ENST00000549224" type="Ensembl">
		//			<property value="ENSP00000447990" type="protein sequence ID" />
		//			<property value="ENSG00000206505" type="gene ID" />
		//		</dbReference>
		// In this case, there are two different dbReference entities that refer to the same ENSEMBL Gene ID. So that's why I have added the .stream().distinct(...)
		// to the for-loop.
		if (data.getEnsembleGeneIDs() != null)
		{
			for (String ensemblGeneID : geneList)
			{
//				boolean speciesModified = false;
				// Check to see if the ENSEMBL ID (Remember: the XSL only selects for "Ensembl" gene names) is in the list of ReferenceDNASequences.
				if (referenceDNASequences.containsKey(ensemblGeneID))
				{
					this.processForExistingENSEMBLID(referenceDNASequences, data, referenceDNASequencesForThisUniprot, geneNames, ensemblGeneID);
				}
				// if the gene ID was NOT in the ReferenceDNASequences map, we may need to add it to the database.
				else
				{
					if (geneList.size() > 1 && !genesOKWithENSEMBL.contains(ensemblGeneID))
					{
						referenceDNASequenceLog.info("{} is not a primary/canonical gene -- skipping creation of ReferenceDNASequence", ensemblGeneID);
					}
					else
					{
						InstanceCreator creator = new InstanceCreator(adaptor, instanceEdit);
						GKInstance newRefDNASequence = creator.createNewReferenceDNASequence(geneNames, primaryGeneName, ensemblGeneID);
						Long newDBID = adaptor.storeInstance(newRefDNASequence);
						InstanceDisplayNameGenerator.setDisplayName(newRefDNASequence);
						adaptor.updateInstanceAttribute(newRefDNASequence, ReactomeJavaConstants._displayName);
						referenceDNASequenceLog.info("New ReferenceDNASequence \"" + newRefDNASequence.toString() + "\" with Gene ID " + ensemblGeneID + " has DB ID" + newDBID);
					}
				}

				if (referenceGeneProducts.containsKey(accession))
				{
					int instanceCount = 0;
					GKInstance referenceGeneProduct = referenceGeneProducts.get(accession);
					if (!referenceGeneProduct.getSchemClass().isa(ReactomeJavaConstants.ReferenceIsoform))
					{
						if (instanceCount < 1)
						{
							instanceCount++;
							this.updateReferenceGeneProduct(referenceGeneProduct, data, accession);
						}
						else
						{
							referenceDNASequenceLog.info("Duplicate ReferenceGeneProduct instance for identifier {} - this instance will NOT be updated.", accession);
						}
					}
				}
				else
				{
					// create new RefGeneProd...
					InstanceCreator creator = new InstanceCreator(adaptor, instanceEdit);
					GKInstance referenceGeneProduct = creator.createNewReferenceGeneProduct(accession);
					Long newRefGeneProductDBID = adaptor.storeInstance(referenceGeneProduct);
					updateInstanceWithData(referenceGeneProduct, data);
					referenceGeneProduct.setAttributeValue(ReactomeJavaConstants.referenceGene, referenceDNASequencesForThisUniprot);
					adaptor.updateInstanceAttribute(referenceGeneProduct, ReactomeJavaConstants.referenceGene);
					InstanceDisplayNameGenerator.setDisplayName(referenceGeneProduct);
					adaptor.updateInstanceAttribute(referenceGeneProduct, ReactomeJavaConstants._displayName);
					uniprotRecordsLog.info("New UniProt: \"{}\" {} {}", referenceGeneProduct.toString(), accession, newRefGeneProductDBID);
					addIsoformsIfNecessary(data, accession, referenceGeneProduct);
				}
			}
		}
	}
	
	/**
	 * Process a UniprotData object for existing ENSEMBL IDs
	 * @param referenceDNASequences - A map of ReferenceDNASequences, keyed by ENSEMBL Gene ID
	 * @param data - the UniprotData object.
	 * @param referenceDNASequencesForThisUniprot - a list of ReferenceDNASequences for the current Uniprot accession. NOTE: This list may be modified (appended to) by this function!!
	 * @param geneNames - A list of gene names for this UniprotData object.
	 * @param ensemblGeneID - The ENSEMBL Gene ID to check for
	 * @throws InvalidAttributeException
	 * @throws Exception
	 * @throws InvalidAttributeValueException
	 */
	private void processForExistingENSEMBLID(Map<String, GKInstance> referenceDNASequences, UniprotData data, List<GKInstance> referenceDNASequencesForThisUniprot, List<String> geneNames, String ensemblGeneID) throws InvalidAttributeException, Exception, InvalidAttributeValueException
	{
		// If this instance already exists in the database, let's update it.
		GKInstance referenceDNASequence = referenceDNASequences.get(ensemblGeneID);
		referenceDNASequencesForThisUniprot.add(referenceDNASequence);
		boolean speciesModified =
				this.updateSpeciesIfNecessary(data, referenceDNASequence,
						InstanceFetcher.getInstanceFetcher(this.adaptor).getHumanSpecies());
		boolean nameModified = this.updateGeneNameIfNecessary(geneNames, ensemblGeneID, referenceDNASequence);
		boolean dbModified = this.setDatabaseIfNecessary(referenceDNASequence);
		boolean instancedWasModified = speciesModified || nameModified || dbModified;
		this.addInstanceEditIfNecessary(ensemblGeneID, instancedWasModified, referenceDNASequence);
	}

	/**
	 * Updates the species of a ReferenceDNASequence, if necessary. An update is deemed necessary if the species name from the database does not match (contain) the scientific species name in the UniprotData object.
	 * In that case, the "species" attribute will be set to <code>newSpecies</code>, and <b>true</b> will be returned to indicate that an update occurred.
	 * 
	 * @param data - the UniprotData object.
	 * @param referenceDNASequence - the ReferenceDNASequence to possibly update.
	 * @param newSpecies - the new species to update to.
	 * @return <b>true</b> if the species was updated.
	 * @throws InvalidAttributeException
	 * @throws InvalidAttributeValueException
	 * @throws Exception
	 */
	private boolean updateSpeciesIfNecessary(UniprotData data, GKInstance referenceDNASequence, GKInstance newSpecies) throws InvalidAttributeException, InvalidAttributeValueException, Exception
	{
		GKInstance speciesFromDB = (GKInstance) referenceDNASequence.getAttributeValue(ReactomeJavaConstants.species);

		@SuppressWarnings("unchecked")
		Set<String> speciesNamesFromDB = new HashSet<>((List<String>) speciesFromDB.getAttributeValuesList(ReactomeJavaConstants.name));

		boolean modified = false;
		// The old Perl code forces the species to be changed if the one in the database does not match the one in the file.
		if (!speciesNamesFromDB.contains(data.getScientificName()))
		{
			referenceDNASequence.setAttributeValue(ReactomeJavaConstants.species, newSpecies);
			adaptor.updateInstanceAttribute(referenceDNASequence, ReactomeJavaConstants.species);
			modified = true;
		}
		return modified;
	}

	/**
	 * Sets the ReferenceDatabase of a ReferenceDNASequence, if necessary. In this case, it is always checking for ENSEMBL_Homo_sapiens_GENE, and setting to that if it's not present.
	 * @param referenceDNASequence - the ReferenceDNASequence to update, if necessary.
	 * @return <b>true</b> if the ReferenceDNASequence was updated.
	 * @throws InvalidAttributeException
	 * @throws Exception
	 * @throws InvalidAttributeValueException
	 */
	private boolean setDatabaseIfNecessary(GKInstance referenceDNASequence) throws InvalidAttributeException, Exception, InvalidAttributeValueException
	{
		boolean modified = false;
		// The old Perl code sets the reference database if it's not ENSEMBL_Homo_sapiens_GENE
		if (!((String) ((GKInstance) referenceDNASequence.getAttributeValue(ReactomeJavaConstants.referenceDatabase)).getAttributeValue(ReactomeJavaConstants.name)).equals(UniprotConstants.ENSEMBL_HOMO_SAPIENS_GENE))
		{
			InstanceFetcher instanceFetcher = InstanceFetcher.getInstanceFetcher(this.adaptor);
			referenceDNASequence.setAttributeValue(ReactomeJavaConstants.referenceDatabase, instanceFetcher.getEnsemblHSapiensRefDB());
			adaptor.updateInstanceAttribute(referenceDNASequence, ReactomeJavaConstants.referenceDatabase);
			modified = true;
		}
		return modified;
	}

	/**
	 * Add a gene name(s) to a ReferenceDNASequence, if it's not already on that RefDNASeq.
	 * @param geneNames - a list of gene names
	 * @param ensemblGeneID - the ENSMEBL Gene ID
	 * @param referenceDNASequence - The ReferenceDNASequence that might be updated
	 * @return <b>true</b> if the instance was updated.
	 * @throws InvalidAttributeException
	 * @throws InvalidAttributeValueException
	 * @throws Exception
	 */
	private boolean updateGeneNameIfNecessary(List<String> geneNames, String ensemblGeneID, GKInstance referenceDNASequence) throws InvalidAttributeException, InvalidAttributeValueException, Exception
	{
		@SuppressWarnings("unchecked")
		Set<String> geneNamesFromDB = new HashSet<>(referenceDNASequence.getAttributeValuesList(ReactomeJavaConstants.geneName));
		
		boolean modified = false;
		// The old Perl code adds the geneName from the file, if it's not already in the database.
		boolean modifiedGeneName = false;
		if (geneNames!=null && !geneNames.isEmpty())
		{
			for (String geneName : geneNames)
			{
				if (!geneNamesFromDB.contains(geneName))
				{
					referenceDNASequence.addAttributeValue(ReactomeJavaConstants.geneName, geneName);
					modified = true;
					modifiedGeneName = true;
				}
			}
		}
		else
		{
			referenceDNASequenceLog.info("UniprotData with ENSEMBL Gene ID {} has empty/NULL flattenedGeneNames!", ensemblGeneID);
		}
		
		if (modifiedGeneName)
		{
			adaptor.updateInstanceAttribute(referenceDNASequence, ReactomeJavaConstants.geneName);
		}
		return modified;
	}

	
	/**
	 * Adds an instance edit to an object in the "modified" attribute, if necessary.
	 * @param ensemblGeneID - the ENSEMBL Gene ID (used only for logging)
	 * @param modified - a flag to indicate if the instances was modified or not.
	 * @param referenceDNASequence - The instance that will get the InstanceEdit, as "modified", if <code>modified==true</code>
	 * @throws InvalidAttributeException
	 * @throws Exception
	 * @throws InvalidAttributeValueException
	 */
	private void addInstanceEditIfNecessary(String ensemblGeneID, boolean modified, GKInstance referenceDNASequence) throws InvalidAttributeException, Exception, InvalidAttributeValueException
	{
		// if the instance was modified, attach a new InstanceEdit to the modified attribute.
		if (modified)
		{
			referenceDNASequence.getAttributeValuesList(ReactomeJavaConstants.modified);
			referenceDNASequence.addAttributeValue(ReactomeJavaConstants.modified, instanceEdit);
			adaptor.updateInstanceAttribute(referenceDNASequence, ReactomeJavaConstants.modified);
			referenceDNASequenceLog.info("Updating existing reference DNA sequence for {} with DB ID: {}", ensemblGeneID, referenceDNASequence.getDBID().toString());
		}
	}



}
