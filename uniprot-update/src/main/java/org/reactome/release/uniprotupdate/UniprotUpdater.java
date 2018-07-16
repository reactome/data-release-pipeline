package org.reactome.release.uniprotupdate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.InstanceEdit;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.MySQLAdaptor.AttributeQueryRequest;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.reactome.release.uniprotupdate.dataschema.Gene;
import org.reactome.release.uniprotupdate.dataschema.Isoform;
import org.reactome.release.uniprotupdate.dataschema.Name;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

/**
 * 
 * @author sshorser
 *
 */
public class UniprotUpdater
{

	private static final String ENSEMBL_HOMO_SAPIENS_GENE = "ENSEMBL_Homo_sapiens_GENE";
	private static final String HOMO_SAPIENS = "Homo sapiens";
	// List of species names was taken from uniprot_xml2sql_isoform.pl:84
	private static final Set<String> speciesToUpdate = new HashSet<String>( Arrays.asList(HOMO_SAPIENS, "Mus musculus", "Rattus norvegicus",
																			"Bos taurus", "Gallus gallus", "Drosophila melanogaster",
																			"Caenorhabditis elegans", "Saccharomyces cerevisiae", "Schizosaccharomyces pombe",
																			"Human immunodeficiency virus type 1", "Human immunodeficiency virus type 2", "Influenza A virus") );
	
	private static GKInstance ensemblHSapiensRefDB;
	private static GKInstance humanSpecies;
	
	private static String geneNamesListToString(List<Gene> geneNames)
	{
		StringBuilder sb = new StringBuilder();
		for (Gene gene : geneNames)
		{
			for (Name name : gene.getNames())
			{
				sb.append("\"").append(name.getValue()).append("\", ");
			}
			
		}
		return sb.toString();
	}
	
	/**
	 * Updates UniProt instances.
	 * @param uniprotData - The uniprot data that was extracted from the XML file. This will be a list of UniprotData objects, each object representing an &lt;entry/&gt; entity from the file.
	 * @param referenceDNASequences - A map of ReferenceDNASequence objects, keyed by their Identifier (ReferenceDNASequences without an identifier should not be in this list).
	 * @param referenceGeneProducts - A map of ReferenceGeneProduct objects, keyed by their Identifier (ReferenceGeneProduct without an identifier should not be in this list).
	 * @param referenceIsoforms - A map of ReferenceIsoforms objects, keyed by their VariantIdentifier (ReferenceIsoforms without an identifier should not be in this list).
	 * @throws Exception 
	 */
	public void updateUniprotInstances(MySQLAdaptor adaptor, List<UniprotData> uniprotData, Map<String, GKInstance> referenceDNASequences, Map<String, GKInstance> referenceGeneProducts, Map<String, GKInstance> referenceIsoforms, InstanceEdit instanceEdit) throws Exception
	{
		synchronized (UniprotUpdater.ensemblHSapiensRefDB)
		{
			// initialize if it's null.
			if (UniprotUpdater.ensemblHSapiensRefDB == null)
			{
				@SuppressWarnings("unchecked")
				List<GKInstance> refDBs = (List<GKInstance>)adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase, ReactomeJavaConstants.name, "=", ENSEMBL_HOMO_SAPIENS_GENE);
				UniprotUpdater.ensemblHSapiensRefDB = refDBs.get(0);
			}
		}
		synchronized (UniprotUpdater.humanSpecies)
		{
			if (UniprotUpdater.humanSpecies == null)
			{
				@SuppressWarnings("unchecked")
				List<GKInstance> species = (List<GKInstance>) adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.species, ReactomeJavaConstants.name, "=", HOMO_SAPIENS);
				UniprotUpdater.humanSpecies = species.get(0);
			}
		}
		
		Map<String,List<String>> secondaryAccessions = new HashMap<String, List<String>>();
		
		for (UniprotData data : uniprotData)
		{
			// first, let's make sure this piece of data is for a species that we can update via Uniprot Update.
			if (speciesToUpdate.contains(data.getScientificName()))
			{
				// Update secondary accessions
				String accession = data.getAccessions().get(0);
				if (secondaryAccessions.containsKey(accession))
				{
					secondaryAccessions.get(accession).addAll( data.getAccessions().subList(1, data.getAccessions().size()) );
				}
				else
				{
					secondaryAccessions.put(accession, data.getAccessions().subList(1, data.getAccessions().size()));
				}
				
				// for human data, we may need to update a ReferenceDNASequence.
				if (data.getScientificName().equals(HOMO_SAPIENS))
				{
					// Will need a flattened list of geneNames. Maybe move this code to a new function in UniprotData?
					List<String> flattenedGeneNames = new ArrayList<String>();
					String primaryGeneName="";
					for (Gene gene : data.getGenes())
					{
						for (Name name : gene.getNames())
						{
							flattenedGeneNames.add(name.getValue());
							if (name.getType().equals("primary"))
							{
								primaryGeneName = name.getValue();
							}
						}
					}
					// If there was no gene name marked as "primary", just take the first one in the list.
					if (primaryGeneName.equals(""))
					{
						primaryGeneName = flattenedGeneNames.get(0);
					}
					// Report when there are multiple gene names.
					if (data.getGenes().size() > 0)
					{
						System.out.println("Accession " + data.getAccessions().toString() + "multiple gene names: " + geneNamesListToString(data.getGenes()));
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
					for (String ensemblGeneID : data.getEnsembleGeneIDs().stream().distinct().collect(Collectors.toList()))
					{
						boolean modified = false;
						// Check to see if the ENSEMBL ID (Remember: the XSL only selects for "Ensembl" gene names) is in the list of ReferenceDNASequences.
						// String geneNameFromFile = name.getValue();
						if (referenceDNASequences.containsKey(ensemblGeneID))
						{
							
							// If this instance already exists in the database, let's update it.
							GKInstance referenceDNASequence = referenceDNASequences.get(ensemblGeneID);
							GKInstance speciesFromDB = (GKInstance) referenceDNASequence.getAttributeValue(ReactomeJavaConstants.Species);
							@SuppressWarnings("unchecked")
							Set<String> speciesNamesFromDB = (Set<String>) speciesFromDB.getAttributeValuesList(ReactomeJavaConstants.name);
							// The old Perl code forces the species to be changed if the one in the database does not match the one in the file.
							if (!speciesNamesFromDB.contains(data.getScientificName()))
							{
								referenceDNASequence.setAttributeValue(ReactomeJavaConstants.species, humanSpecies);
								adaptor.updateInstanceAttribute(referenceDNASequence, ReactomeJavaConstants.species);
								modified = true;
							}
							
							@SuppressWarnings("unchecked")
							Set<String> geneNamesFromDB = (Set<String>)referenceDNASequence.getAttributeValuesList(ReactomeJavaConstants.geneName);
							// The old Perl code adds the geneName from the file, if it's not already in the database.
							
							
							// The old Perl code sets the reference database if it's not ENSEMBL_Homo_sapiens_GENE
							if (!((String)((GKInstance)referenceDNASequence.getAttributeValue(ReactomeJavaConstants.referenceDatabase)).getAttributeValue(ReactomeJavaConstants.name)).equals(UniprotUpdater.ENSEMBL_HOMO_SAPIENS_GENE) )
							{
								referenceDNASequence.setAttributeValue(ReactomeJavaConstants.referenceDatabase, UniprotUpdater.ensemblHSapiensRefDB);
								adaptor.updateInstanceAttribute(referenceDNASequence, ReactomeJavaConstants.referenceDatabase);
								modified = true;
							}
							// if the instance was modified, attach a new InstanceEdit to the modified attribute.
							if (modified)
							{
								referenceDNASequence.getAttributeValuesList(ReactomeJavaConstants.modified);
								referenceDNASequence.addAttributeValue(ReactomeJavaConstants.modified, instanceEdit);
								adaptor.updateInstanceAttribute(referenceDNASequence, ReactomeJavaConstants.modified);
							}
							

						}
						// if the gene ID was NOT in the ReferenceDNASequences map, we may need to add it to the database.
						else
						{

							GKInstance newRefDNASequence = new GKInstance(adaptor.getSchema().getClassByName(ReactomeJavaConstants.ReferenceDNASequence));
							newRefDNASequence.setAttributeValue(ReactomeJavaConstants.identifier, ensemblGeneID);
							newRefDNASequence.setAttributeValue(ReactomeJavaConstants.species, humanSpecies);
							newRefDNASequence.setAttributeValue(ReactomeJavaConstants.referenceDatabase, UniprotUpdater.ensemblHSapiensRefDB);
							newRefDNASequence.setAttributeValue(ReactomeJavaConstants.geneName, flattenedGeneNames);
							newRefDNASequence.setAttributeValue(ReactomeJavaConstants.created, instanceEdit);
							newRefDNASequence.setAttributeValue(ReactomeJavaConstants.modified, null);
							newRefDNASequence.setAttributeValue(ReactomeJavaConstants.name, primaryGeneName);
							Long newDBID = adaptor.storeInstance(newRefDNASequence);
							System.out.println("New ReferenceDNASequence \""+newRefDNASequence.toString()+"\" with Gene ID "+ensemblGeneID+" has DB ID"+newDBID);
						}
						// For all species...
						// Process the rest of the data - chains, isoforms...

						AttributeQueryRequest aqrIdentifier = adaptor.new AttributeQueryRequest(ReactomeJavaConstants.ReferenceGeneProduct, ReactomeJavaConstants.identifier, "=", accession);
						AttributeQueryRequest aqrRefDB = adaptor.new AttributeQueryRequest(ReactomeJavaConstants.ReferenceGeneProduct, ReactomeJavaConstants.referenceDatabase, "=", UniprotUpdater.ensemblHSapiensRefDB);
						List<AttributeQueryRequest> aqrList = Arrays.asList(aqrIdentifier, aqrRefDB);
						@SuppressWarnings("unchecked")
						List<GKInstance> refGeneProductsFromDB = (List<GKInstance>) adaptor._fetchInstance(aqrList);
						// Update existing ReferenceGeneProduct
						if (refGeneProductsFromDB.size() > 0)
						{
							for (GKInstance referenceGeneProduct : refGeneProductsFromDB)
							{
								if (!referenceGeneProduct.getSchemClass().isa(ReactomeJavaConstants.ReferenceIsoform))
								{
									// TODO: Check for duplicates, see: uniprot_xml2sql_isoform.sql:434-438
									updateReferenceGeneProduct(adaptor, referenceGeneProduct, data, instanceEdit, accession);
								}
							}
						}	
						else
						{
							//create new RefGeneProd...
							GKInstance referenceGeneProduct = new GKInstance(adaptor.getSchema().getClassByName(ReactomeJavaConstants.ReferenceGeneProduct));
							referenceGeneProduct.setAttributeValue(ReactomeJavaConstants.created, instanceEdit);
							referenceGeneProduct.setAttributeValue(ReactomeJavaConstants.modified, instanceEdit);
							referenceGeneProduct.setAttributeValue(ReactomeJavaConstants.referenceDatabase, UniprotUpdater.ensemblHSapiensRefDB);
							referenceGeneProduct.setAttributeValue(ReactomeJavaConstants.identifier, accession);
							// update with the rest of the values in "data"... TODO: complete something that is like the Perl version of uniprot_xml2sql_isoform::updateInstance
							Long newRefGeneProductDBID = adaptor.storeInstance(referenceGeneProduct);
							// Now create new ReferenceIsoform for this ReferenceGeneProduct.
							for (Isoform isoform : data.getIsoforms())
							{
								String isoformID = isoform.getIsoformID();
								// Check to see if isoformID == accession - it should happen!
								if (isoformID.contains(accession))
								{
									createNewReferenceIsoform(adaptor, instanceEdit, accession, referenceGeneProduct, isoformID);
								}
								else
								{
									// log an error about mismatched isoform ID and accession.
								}
							}
						}
					}
				}
			}
		}
	}

	private void createNewReferenceIsoform(MySQLAdaptor adaptor, InstanceEdit instanceEdit, String accession, GKInstance referenceGeneProduct, String isoformID) throws InvalidAttributeException, InvalidAttributeValueException, Exception
	{
		GKInstance referenceIsoform = new GKInstance(adaptor.getSchema().getClassByName(ReactomeJavaConstants.ReferenceIsoform));
		referenceIsoform.setAttributeValue(ReactomeJavaConstants.created, instanceEdit);
		referenceIsoform.setAttributeValue(ReactomeJavaConstants.identifier, accession);
		referenceIsoform.setAttributeValue(ReactomeJavaConstants.referenceDatabase, UniprotUpdater.ensemblHSapiensRefDB);
		referenceIsoform.setAttributeValue(ReactomeJavaConstants.isoformParent, referenceGeneProduct);
		referenceIsoform.setAttributeValue(ReactomeJavaConstants.variantIdentifier, isoformID);
		// now update with the rest of the values in "data"...
		adaptor.storeInstance(referenceIsoform);
	}

	private void updateReferenceGeneProduct(MySQLAdaptor adaptor, GKInstance referenceGeneProduct, UniprotData data, InstanceEdit instanceEdit, String accession) throws Exception
	{
		// TODO: add code to check for duplicates.
		// Update with other values from "data"...
		
		String isoformID = ""; //TODO: loop through isoforms of this refeference gene product.
		updateIsoforms(adaptor, referenceGeneProduct, data.getIsoforms(), instanceEdit, accession, isoformID);
	}
	
	private void updateIsoforms(MySQLAdaptor adaptor, GKInstance referenceGeneProduct, List<Isoform> isoforms, InstanceEdit instanceEdit, String accession, String isoformID) throws Exception
	{
		for (Isoform isoform : isoforms)
		{
			@SuppressWarnings("unchecked")
			List<GKInstance> refIsoformsFromDB = (List<GKInstance>) adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceIsoform, ReactomeJavaConstants.variantIdentifier, "=", isoform.getIsoformID());
			if (isoformID.contains(accession))
			{
				// Update existing ReferenceIsoforms
				if (refIsoformsFromDB.size() > 0)
				{
					for (GKInstance refIsoformFromDB : refIsoformsFromDB)
					{
						if (((String)refIsoformFromDB.getAttributeValue(ReactomeJavaConstants.variantIdentifier)).equals(isoformID))
						{
							// update instance with values...
						}
					}
				}
				// Create a shiny *NEW* ReferenceIsoform!
				else
				{
					createNewReferenceIsoform(adaptor, instanceEdit, accession, referenceGeneProduct, isoformID);
				}
			}
			else
			{
				// log a message about mismatches...
			}
		}
	}
}
