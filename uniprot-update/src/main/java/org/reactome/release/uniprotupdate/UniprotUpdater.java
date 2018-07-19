package org.reactome.release.uniprotupdate;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.MySQLAdaptor.AttributeQueryRequest;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.reactome.release.uniprotupdate.dataschema.Chain;
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

	
	private static final Logger logger = LogManager.getLogger();
	private static final Logger uniprotRecordsLog = LogManager.getLogger("uniprotRecordsLog");
	private static final Logger sequencesLog = LogManager.getLogger("sequencesLog");
	private static final Logger referenceDNASequenceLog = LogManager.getLogger("referenceDNASequenceLog");
	
	private static final String CHAIN_CHANGE_LOG = "_chainChangeLog";
	private static final String ENSEMBL_HOMO_SAPIENS_GENE = "ENSEMBL_Homo_sapiens_GENE";
	private static final String HOMO_SAPIENS = "Homo sapiens";
	// List of species names was taken from uniprot_xml2sql_isoform.pl:84
	private static final Set<String> speciesToUpdate = new HashSet<String>( Arrays.asList(HOMO_SAPIENS, "Mus musculus", "Rattus norvegicus",
																			"Bos taurus", "Gallus gallus", "Drosophila melanogaster",
																			"Caenorhabditis elegans", "Saccharomyces cerevisiae", "Schizosaccharomyces pombe",
																			"Human immunodeficiency virus type 1", "Human immunodeficiency virus type 2", "Influenza A virus") );
	
	private static GKInstance ensemblHSapiensRefDB;
	private static GKInstance humanSpecies;
	
	private static String geneNamesListToString(Collection<Gene> geneNames)
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
	public void updateUniprotInstances(MySQLAdaptor adaptor, List<UniprotData> uniprotData, Map<String, GKInstance> referenceDNASequences, Map<String, GKInstance> referenceGeneProducts, Map<String, GKInstance> referenceIsoforms, GKInstance instanceEdit) throws Exception
	{
		synchronized (this)
		{
			// initialize if it's null.
			if (UniprotUpdater.ensemblHSapiensRefDB == null)
			{
				@SuppressWarnings("unchecked")
				List<GKInstance> refDBs = new ArrayList<GKInstance>((Collection<GKInstance>)adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase, ReactomeJavaConstants.name, "=", ENSEMBL_HOMO_SAPIENS_GENE));
				UniprotUpdater.ensemblHSapiensRefDB = refDBs.get(0);
			}
		}
		synchronized (this)
		{
			if (UniprotUpdater.humanSpecies == null)
			{
				@SuppressWarnings("unchecked")
				List<GKInstance> species = new ArrayList<GKInstance>((Collection<GKInstance>)adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Species, ReactomeJavaConstants.name, "=", HOMO_SAPIENS));
				UniprotUpdater.humanSpecies = species.get(0);
			}
		}
		
		Map<String,List<String>> secondaryAccessions = new HashMap<String, List<String>>();
		int i = 0;
		for (UniprotData data : uniprotData)
		{
			i++;
			if (i%10000 == 0)
			{
				logger.info("{} Uniprot data records processed...",i);
			}
			// Should each pass through this loop be a single transaction? This might work well if this loop is run in parallel...
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
					
					flattenedGeneNames = data.getFlattenedGeneNames();
					if (flattenedGeneNames != null && !flattenedGeneNames.isEmpty())
					{
						primaryGeneName = flattenedGeneNames.get(0);
					}
							
					
					// If there was no gene name marked as "primary", just take the first one in the list.
					if (primaryGeneName.equals("") && flattenedGeneNames != null && !flattenedGeneNames.isEmpty())
					{
						primaryGeneName = flattenedGeneNames.get(0);
					}
					// Report when there are multiple gene names.
					if (data.getGenes() != null && data.getGenes().size() > 0)
					{
						referenceDNASequenceLog.info("Accession " + data.getAccessions().toString() + "multiple gene names: " + geneNamesListToString(data.getGenes()));
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
					if (data.getEnsembleGeneIDs()!=null)
					{
						for (String ensemblGeneID : data.getEnsembleGeneIDs().stream().distinct().collect(Collectors.toList()))
						{
							boolean modified = false;
							// Check to see if the ENSEMBL ID (Remember: the XSL only selects for "Ensembl" gene names) is in the list of ReferenceDNASequences.
							// String geneNameFromFile = name.getValue();
							if (referenceDNASequences.containsKey(ensemblGeneID))
							{
								
								// If this instance already exists in the database, let's update it.
								GKInstance referenceDNASequence = referenceDNASequences.get(ensemblGeneID);
								GKInstance speciesFromDB = (GKInstance) referenceDNASequence.getAttributeValue(ReactomeJavaConstants.species);
								@SuppressWarnings("unchecked")
								Set<String> speciesNamesFromDB = new HashSet<String>((List<String>) speciesFromDB.getAttributeValuesList(ReactomeJavaConstants.name));
								// The old Perl code forces the species to be changed if the one in the database does not match the one in the file.
								if (!speciesNamesFromDB.contains(data.getScientificName()))
								{
									referenceDNASequence.setAttributeValue(ReactomeJavaConstants.species, humanSpecies);
									adaptor.updateInstanceAttribute(referenceDNASequence, ReactomeJavaConstants.species);
									modified = true;
								}
								
								@SuppressWarnings("unchecked")
								Set<String> geneNamesFromDB = new HashSet<String>((List<String>)referenceDNASequence.getAttributeValuesList(ReactomeJavaConstants.geneName));
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
	
								GKInstance newRefDNASequence = createNewReferenceDNASequence(adaptor, instanceEdit, flattenedGeneNames, primaryGeneName, ensemblGeneID);
								Long newDBID = adaptor.storeInstance(newRefDNASequence);
								InstanceDisplayNameGenerator.setDisplayName(newRefDNASequence);
								referenceDNASequenceLog.info("New ReferenceDNASequence \""+newRefDNASequence.toString()+"\" with Gene ID "+ensemblGeneID+" has DB ID"+newDBID);
							}
							// For all species...
							// Process the rest of the data - chains, isoforms...
	
							// We need to query for ReferenceGeneProducts that have a specific identifier and are also in the "ENSEMBL - Human" reference database.
							AttributeQueryRequest aqrIdentifier = adaptor.new AttributeQueryRequest(ReactomeJavaConstants.ReferenceGeneProduct, ReactomeJavaConstants.identifier, "=", accession);
							AttributeQueryRequest aqrRefDB = adaptor.new AttributeQueryRequest(ReactomeJavaConstants.ReferenceGeneProduct, ReactomeJavaConstants.referenceDatabase, "=", UniprotUpdater.ensemblHSapiensRefDB);
							List<AttributeQueryRequest> aqrList = Arrays.asList(aqrIdentifier, aqrRefDB);
							@SuppressWarnings("unchecked")
							List<GKInstance> refGeneProductsFromDB = new ArrayList<GKInstance>((Set<GKInstance>) adaptor._fetchInstance(aqrList));
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
								GKInstance referenceGeneProduct = createNewReferenceGeneProduct(adaptor, instanceEdit, accession);
								Long newRefGeneProductDBID = adaptor.storeInstance(referenceGeneProduct);
								InstanceDisplayNameGenerator.setDisplayName(referenceGeneProduct);
								updateInstanceWithData(adaptor, referenceGeneProduct, data);
								// Now create new ReferenceIsoform for this ReferenceGeneProduct.
								if (data.getIsoforms()!=null)
								{
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
											referenceDNASequenceLog.info("Isoform ID "+ isoformID + " does not match Accession "+accession);
											
											// Update mismatched Isoforms
											updateMismatchedIsoform(adaptor, isoformID, accession);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void updateMismatchedIsoform(MySQLAdaptor adaptor, String isoformID, String accession) throws Exception
	{
		// Again, I really don't expect more than 1 to be returned, but still need to treat this as a collection.
		@SuppressWarnings("unchecked")
		Set<GKInstance> isoformsFromDB = (Set<GKInstance>) adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceIsoform, ReactomeJavaConstants.variantIdentifier, "=", isoformID);
//		GKInstance isoformsFromDBTmp = (GKInstance) adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceIsoform, ReactomeJavaConstants.variantIdentifier, "=", isoformID);
//		Collection<GKInstance> isoformsFromDB = new ArrayList<GKInstance>();
//		isoformsFromDB.add(isoformsFromDBTmp);
		List<GKInstance> allParents = new ArrayList<GKInstance>();
		if (isoformsFromDB != null && !isoformsFromDB.isEmpty())
		{
			for (GKInstance isoformFromDB : isoformsFromDB)
			{
				// Get the current values for "isoformParent" for the isoform.
				@SuppressWarnings("unchecked")
				GKInstance isoformParents = (GKInstance) isoformFromDB.getAttributeValue(ReactomeJavaConstants.isoformParent);
				if (isoformParents!=null /*&& !isoformParents.isEmpty()*/)
				{
					allParents.add(isoformParents);
				}
				// Get the ReferenceGeneProduct(s) by accession (probably should only return 1, but who knows? I don't think anything enforces RGPs to have unique accessions).
				@SuppressWarnings("unchecked")
				Set<GKInstance> referenceGeneProducts = (Set<GKInstance>) adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct, ReactomeJavaConstants.identifier, "=", accession);
				if (referenceGeneProducts!=null && !referenceGeneProducts.isEmpty())
				{
					allParents.addAll(referenceGeneProducts);
				}
				// print a message. This was copied from the Perl implementation, but I do not think it is very clear. Need a better message!
				referenceDNASequenceLog.info("Mismatched parent: "+isoformID+" ("+isoformFromDB.getDBID()+")\t"+accession);
				// Now correct isoformParent
				isoformFromDB.setAttributeValue(ReactomeJavaConstants.isoformParent, allParents);
				adaptor.updateInstanceAttribute(isoformFromDB, ReactomeJavaConstants.isoformParent);
			}
		}
	}

	/**
	 * This function will update attributes on an instance, based on the content of a UniprotData object.
	 * @param adaptor - The database adaptor to use.
	 * @param instance - The instance to try updating.
	 * @param data - The UniprotData object that may contain new data for the instance.
	 * @param attributes - A list of attributes to update. If you do not pass a list, the default list (everything!) will be attempted:
	 * <ul>
	 * <li>secondaryIdentifier</li><li>description</li><li>sequenceLength</li><li>species</li><li>checksum</li><li>name</li><li>geneName</li>
	 * <li>comment</li><li>keyword</li><li>chain</li>
	 * </ul>
	 * If you pass in your own list of attributes, <b>make sure they are valid</b> for <code>instance</code>! This function does <em>not</em> check attribute
	 * validity.
	 */
	private void updateInstanceWithData(MySQLAdaptor adaptor, GKInstance instance, UniprotData data, String ... attributes )
	{
		// Set the default list if the user does not specify anything.
		if (attributes == null || attributes.length == 0)
		{
			attributes = new String[] { ReactomeJavaConstants.secondaryIdentifier, ReactomeJavaConstants.description, ReactomeJavaConstants.sequenceLength,
										ReactomeJavaConstants.species, "checksum", ReactomeJavaConstants.name, ReactomeJavaConstants.geneName,
										ReactomeJavaConstants.comment, ReactomeJavaConstants.keyword, "chain" };
		}
		//if (attributes!=null && attributes.length > 0) {
		for (String attribute : attributes )
		{
			// The old Perl code actually prints messages every time the old data differs from the new data. Is that really necessary?
			try
			{
				switch (attribute)
				{
					case ReactomeJavaConstants.secondaryIdentifier:
					{
						if (data.getAccessions()!=null && data.getAccessions().size()>0)
						{
							instance.setAttributeValue(ReactomeJavaConstants.secondaryIdentifier, data.getAccessions());
							adaptor.updateInstanceAttribute(instance, ReactomeJavaConstants.secondaryIdentifier);
						}
						break;
					}
					case ReactomeJavaConstants.description:
					{
						if (data.getRecommendedName()!=null)
						{
							instance.setAttributeValue(ReactomeJavaConstants.description, data.getRecommendedName());
							adaptor.updateInstanceAttribute(instance, ReactomeJavaConstants.description);
						}
						break;
					}
					case ReactomeJavaConstants.sequenceLength:
					{
						if (data.getSequenceLength()!=null)
						{
							instance.setAttributeValue(ReactomeJavaConstants.sequenceLength, new Integer(data.getSequenceLength()));
							adaptor.updateInstanceAttribute(instance, ReactomeJavaConstants.sequenceLength);
						}
						break;
					}
					case ReactomeJavaConstants.species:
					{
						String speciesName = data.getScientificName();
						try
						{
							// Using a list here because that's what fetchInstanceByAttribute returns but I honestly don't expect more than one result.
							// It would be *very* weird if two different Species objects existed with the same name.
							// TODO: Maybe introduce a cache for species, keyed by name. Might save a little time on looking things up in the database.
							@SuppressWarnings("unchecked")
							List<GKInstance> dataSpeciesInst = new ArrayList<GKInstance>((Set<GKInstance>) adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Species, ReactomeJavaConstants.name, "=", speciesName));
							Set<Long> speciesDBIDs = new HashSet<Long>();
							for (GKInstance inst : dataSpeciesInst)
							{
								speciesDBIDs.add(inst.getDBID());
							}
							GKInstance speciesInst = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.species);
							// The list of Species that we got by looking up the name from "data" does not contain the Species DB ID on the current instance. 
							// This means we need to update the instance to use the one from the input.
							// does it make sense in the data model for speciesInst to be null?
							if (speciesInst!=null && !speciesDBIDs.contains(speciesInst.getDBID()))
							{
								instance.setAttributeValue(ReactomeJavaConstants.species, dataSpeciesInst.get(0));
								adaptor.updateInstanceAttribute(instance, ReactomeJavaConstants.species);
							}
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
						break;
					}
					case "checksum":
					{
						try
						{
							String oldChecksum = (String) instance.getAttributeValue("checksum");
							if (oldChecksum != null && oldChecksum.length() > 0 && data.getSequenceChecksum().equals(oldChecksum))
							{
								// The old Perl code prints a warning when the checksum changes.
								sequencesLog.info("Checksum has changed! DB ID: "+instance.getDBID() + "\tOld checksum: "+oldChecksum+"\tNew checksum:"+data.getSequenceChecksum());
								instance.setAttributeValue("isSequenceChanged", true);
								instance.setAttributeValue("checksum", data.getSequenceChecksum());
								adaptor.updateInstanceAttribute(instance, "isSequenceChanged");
								adaptor.updateInstanceAttribute(instance, "checksum");
							}
							else
							{
								instance.setAttributeValue("isSequenceChanged", false);
								adaptor.updateInstanceAttribute(instance,"isSequenceChanged");
							}
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
						break;
					}
					case ReactomeJavaConstants.name:
					{
						
						if (data.getFlattenedGeneNames()!=null && data.getFlattenedGeneNames().size() > 0)
						{
							// The first item in the flattened gene names list is the primary gene name.
							instance.setAttributeValue(ReactomeJavaConstants.name,  data.getFlattenedGeneNames().get(0));
							adaptor.updateInstanceAttribute(instance, ReactomeJavaConstants.name);
						}
						break;
					}
					case ReactomeJavaConstants.geneName:
					{
						if (data.getFlattenedGeneNames()!=null && data.getFlattenedGeneNames().size() > 0)
						{
							instance.setAttributeValue(ReactomeJavaConstants.geneName, data.getFlattenedGeneNames());
							adaptor.updateInstanceAttribute(instance, ReactomeJavaConstants.geneName);
						}
						break;
					}
					case ReactomeJavaConstants.comment:
					{
						if (!data.getFlattenedCommentsText().isEmpty())
						{
							instance.setAttributeValue(ReactomeJavaConstants.comment, data.getFlattenedCommentsText());
							adaptor.updateInstanceAttribute(instance, ReactomeJavaConstants.comment);
						}
						break;
					}
					case ReactomeJavaConstants.keyword:
					{
						if (!data.getFlattenedKeywords().isEmpty())
						{
							instance.setAttributeValue(ReactomeJavaConstants.keyword, data.getFlattenedKeywords());
							adaptor.updateInstanceAttribute(instance, ReactomeJavaConstants.keyword);
						}
						break;
					}
					case "chain":
					{
						if (data.getChains()!=null && data.getChains().size() > 0)
						{
							List<String> chainStrings = new ArrayList<String>();
							for (Chain chain : data.getChains())
							{
								chainStrings.add(chain.toString());
							}
							logChainChanges(adaptor, new HashSet<String>(chainStrings), instance);
							instance.setAttributeValue("chain", chainStrings);
							adaptor.updateInstanceAttribute(instance, "chain");
							// TODO: implement "update_chain_log" functionality.
							
						}
						break;
					}
				}
			}
			catch (InvalidAttributeException | InvalidAttributeValueException e)
			{
				e.printStackTrace();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
	}
	
	private void logChainChanges(MySQLAdaptor adaptor, Set<String> newChains, GKInstance instance) throws Exception
	{
		@SuppressWarnings("unchecked")
		Set<String> oldChains = new HashSet<String>((List<String>) instance.getAttributeValuesList("chain"));
		boolean needsUpdate = false;
		LocalDateTime currentDate = LocalDateTime.now();
		// Perl code was:
		// my $t = localtime;
		// my $date = $t->day . ' ' . $t->fullmonth . ' ' . $t->mday . ' ' . $t->year;
		// See "Patterns for Formatting and Parsing" on this page: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE LLLL dd yyyy");
		String dateString = formatter.toFormat().format(currentDate);

		String priorLog = (String) instance.getAttributeValue(CHAIN_CHANGE_LOG);
		if (priorLog == null)
		{
			priorLog = "";
		}
		String logEntry = priorLog;
		for (String oldChain : oldChains)
		{
			if (!newChains.contains(oldChain))
			{
				logEntry = (logEntry.trim().equals("") ? "" : logEntry + " ; ") + oldChain + " for " + instance.getDBID().longValue() + " removed on "+dateString;
				needsUpdate = true;
			}
		}
		
		for (String newChain : newChains)
		{
			if (!oldChains.contains(newChain))
			{
				logEntry = (logEntry.trim().equals("") ? "" : logEntry + " ; ") + newChain + " for " + instance.getDBID().longValue() + " added on "+dateString;
				needsUpdate = true;
			}
		}
		
		if (needsUpdate)
		{
			sequencesLog.info("Chain differences: {} ; For: {}", logEntry, instance.toString());
			instance.setAttributeValue(CHAIN_CHANGE_LOG, logEntry);
			adaptor.updateInstanceAttribute(instance, CHAIN_CHANGE_LOG);
		}
	}

	private GKInstance createNewReferenceGeneProduct(MySQLAdaptor adaptor, GKInstance instanceEdit, String accession) throws InvalidAttributeException, InvalidAttributeValueException
	{
		GKInstance referenceGeneProduct = new GKInstance(adaptor.getSchema().getClassByName(ReactomeJavaConstants.ReferenceGeneProduct));
		referenceGeneProduct.setAttributeValue(ReactomeJavaConstants.created, instanceEdit);
		referenceGeneProduct.setAttributeValue(ReactomeJavaConstants.modified, instanceEdit);
		referenceGeneProduct.setAttributeValue(ReactomeJavaConstants.referenceDatabase, UniprotUpdater.ensemblHSapiensRefDB);
		referenceGeneProduct.setAttributeValue(ReactomeJavaConstants.identifier, accession);
		referenceGeneProduct.setDbAdaptor(adaptor);
		return referenceGeneProduct;
	}

	private GKInstance createNewReferenceDNASequence(MySQLAdaptor adaptor, GKInstance instanceEdit, List<String> flattenedGeneNames, String primaryGeneName, String ensemblGeneID) throws InvalidAttributeException, InvalidAttributeValueException
	{
		GKInstance newRefDNASequence = new GKInstance(adaptor.getSchema().getClassByName(ReactomeJavaConstants.ReferenceDNASequence));
		newRefDNASequence.setAttributeValue(ReactomeJavaConstants.identifier, ensemblGeneID);
		newRefDNASequence.setAttributeValue(ReactomeJavaConstants.species, humanSpecies);
		newRefDNASequence.setAttributeValue(ReactomeJavaConstants.referenceDatabase, UniprotUpdater.ensemblHSapiensRefDB);
		newRefDNASequence.setAttributeValue(ReactomeJavaConstants.geneName, flattenedGeneNames);
		newRefDNASequence.setAttributeValue(ReactomeJavaConstants.created, instanceEdit);
		newRefDNASequence.setAttributeValue(ReactomeJavaConstants.modified, null);
		newRefDNASequence.setAttributeValue(ReactomeJavaConstants.name, primaryGeneName);
		newRefDNASequence.setDbAdaptor(adaptor);
		return newRefDNASequence;
	}

	private Long createNewReferenceIsoform(MySQLAdaptor adaptor, GKInstance instanceEdit, String accession, GKInstance referenceGeneProduct, String isoformID) throws InvalidAttributeException, InvalidAttributeValueException, Exception
	{
		Long dbID;
		GKInstance referenceIsoform = new GKInstance(adaptor.getSchema().getClassByName(ReactomeJavaConstants.ReferenceIsoform));
		referenceIsoform.setAttributeValue(ReactomeJavaConstants.created, instanceEdit);
		referenceIsoform.setAttributeValue(ReactomeJavaConstants.identifier, accession);
		referenceIsoform.setAttributeValue(ReactomeJavaConstants.referenceDatabase, UniprotUpdater.ensemblHSapiensRefDB);
		referenceIsoform.setAttributeValue(ReactomeJavaConstants.isoformParent, referenceGeneProduct);
		referenceIsoform.setAttributeValue(ReactomeJavaConstants.variantIdentifier, isoformID);
		referenceIsoform.setDbAdaptor(adaptor);
		// now update with the rest of the values in "data"...
		dbID = adaptor.storeInstance(referenceIsoform);
		InstanceDisplayNameGenerator.setDisplayName(referenceIsoform);
		return dbID;
	}

	private void updateReferenceGeneProduct(MySQLAdaptor adaptor, GKInstance referenceGeneProduct, UniprotData data, GKInstance instanceEdit, String accession) throws Exception
	{
		// TODO: add code to check for duplicates.
		updateInstanceWithData(adaptor, referenceGeneProduct, data);
		String isoformID = ""; //TODO: loop through isoforms of this refeference gene product.
		updateIsoforms(adaptor, referenceGeneProduct, data.getIsoforms(), instanceEdit, accession, isoformID, data);
		
	}
	
	private void updateIsoforms(MySQLAdaptor adaptor, GKInstance referenceGeneProduct, List<Isoform> isoforms, GKInstance instanceEdit, String accession, String isoformID, UniprotData data) throws Exception
	{
		if(isoforms != null)
		{
			for (Isoform isoform : isoforms)
			{
				@SuppressWarnings("unchecked")
				List<GKInstance> refIsoformsFromDB = new ArrayList<GKInstance>((Set<GKInstance>) adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceIsoform, ReactomeJavaConstants.variantIdentifier, "=", isoform.getIsoformID()));
				if (isoformID.contains(accession))
				{
					// Update existing ReferenceIsoforms
					if (refIsoformsFromDB.size() > 0)
					{
						for (GKInstance refIsoformFromDB : refIsoformsFromDB)
						{
							if (((String)refIsoformFromDB.getAttributeValue(ReactomeJavaConstants.variantIdentifier)).equals(isoformID))
							{
								updateInstanceWithData(adaptor, refIsoformFromDB, data);
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
					referenceDNASequenceLog.info("Isoform ID "+isoformID + " does not match with Accession "+accession);
					// Update mismatched Isoforms
					updateMismatchedIsoform(adaptor, isoformID, accession);
				}
			}
		}
	}
	
	public void deleteObsoleteInstances(MySQLAdaptor adaptor, String pathToUnreviewedUniprotIDsFile) throws Exception
	{
		// starting size for set determined by `wc -l uniprot-reviewed\:no.list`
		Set<String> unreviewedUniprotIDs = Collections.synchronizedSet(new HashSet<String>(13000000));
		
		Files.readAllLines(Paths.get(pathToUnreviewedUniprotIDsFile)).parallelStream().forEach( line -> unreviewedUniprotIDs.add(line));
		
		@SuppressWarnings("unchecked")
		Collection<GKInstance> allReferenceGeneProducts = (Collection<GKInstance>)adaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceGeneProduct);
		
		for (GKInstance referenceGeneProduct : allReferenceGeneProducts)
		{
			String accession = (String) referenceGeneProduct.getAttributeValue(ReactomeJavaConstants.accession);
			// If there's no variantIdentifier and the accession is NOT in the unreviewed UniprotIDs list, we can try to delete the ReferenceGeneProduct if it has no referrers.
			if (referenceGeneProduct.getAttributeValue(ReactomeJavaConstants.variantIdentifier) == null && !unreviewedUniprotIDs.contains(accession));
			{
				@SuppressWarnings("unchecked")
				Collection<GKSchemaAttribute> referringAttributes = (Collection<GKSchemaAttribute>) referenceGeneProduct.getSchemClass().getReferers();
				int referrerCount = 0;
				for (GKSchemaAttribute referringAttribute : referringAttributes)
				{
					@SuppressWarnings("unchecked")
					Collection<GKInstance> referrers = (Collection<GKInstance>) referenceGeneProduct.getReferers(referringAttribute);
					referrerCount += referrers.size();
				}
				if (referrerCount == 0)
				{
					referenceDNASequenceLog.info("ReferenceGeneProduct "+referenceGeneProduct.toString() + " has 0 referrers, no variantIdentifier, and is not in the unreviewed Uniprot IDs list, so it will be deleted.");
					adaptor.deleteByDBID(referenceGeneProduct.getDBID());
				}
			}
		}
	}
}
