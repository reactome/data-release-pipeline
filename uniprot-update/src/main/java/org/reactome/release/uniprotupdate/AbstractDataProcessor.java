package org.reactome.release.uniprotupdate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.reactome.release.uniprotupdate.dataschema.Chain;
import org.reactome.release.uniprotupdate.dataschema.Gene;
import org.reactome.release.uniprotupdate.dataschema.Isoform;
import org.reactome.release.uniprotupdate.dataschema.Name;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

abstract class AbstractDataProcessor
{ 
	MySQLAdaptor adaptor;
	GKInstance instanceEdit;
	static final String CHAIN_CHANGE_LOG = "_chainChangeLog";
	static Map<String, List<GKInstance>> speciesCache = new HashMap<>();
	static final Logger logger = LogManager.getLogger();
	static final Logger uniprotRecordsLog = LogManager.getLogger("uniprotRecordsLog");
	static final Logger sequencesLog = LogManager.getLogger("sequencesLog");
	static final Logger referenceDNASequenceLog = LogManager.getLogger("referenceDNASequenceLog");

	/**
	 * This function will update attributes on an instance, based on the content of a UniprotData object.
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
	protected void updateInstanceWithData(GKInstance instance, UniprotData data, String... attributes)
	{
		// Set the default list if the user does not specify anything.
		String[] attributesToUse;
		if (attributes == null || attributes.length == 0)
		{
			attributesToUse = new String[] { ReactomeJavaConstants.secondaryIdentifier, ReactomeJavaConstants.description, ReactomeJavaConstants.sequenceLength, ReactomeJavaConstants.species, "checksum", ReactomeJavaConstants.name, ReactomeJavaConstants.geneName, ReactomeJavaConstants.comment,
					ReactomeJavaConstants.keyword, "chain" };
		}
		else
		{
			attributesToUse = attributes;
		}
		// if (attributes!=null && attributes.length > 0) {
		for (String attribute : attributesToUse)
		{
			// The old Perl code actually prints messages every time the old data differs from the new data. Is that really necessary?
			try
			{
				updateInstanceForAttribute(instance, data, attribute);
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
	
	/**
	 * Update an instance for some attribute.
	 * @param adaptor - the database adaptor to use
	 * @param instance - some Instance
	 * @param data - A UniprotData object, from which the updates will be made.
	 * @param attribute - can be one of: ReactomeJavaConstants.secondaryIdentifier, ReactomeJavaConstants.description, ReactomeJavaConstants.sequenceLength, ReactomeJavaConstants.species, "checksum", ReactomeJavaConstants.name, ReactomeJavaConstants.geneName, ReactomeJavaConstants.comment, ReactomeJavaConstants.keyword, "chain" 
	 * @throws InvalidAttributeException
	 * @throws InvalidAttributeValueException
	 * @throws Exception
	 */
	protected void updateInstanceForAttribute(GKInstance instance, UniprotData data, String attribute) throws InvalidAttributeException, InvalidAttributeValueException, Exception
	{
		// TODO: this method, and all of the methods that were extracted from it, should probably be moved to a new class, UniprotInstanceUpdater, whose job is to update a GKInstance.
		
		switch (attribute)
		{
			case ReactomeJavaConstants.secondaryIdentifier:
			{
				if (data.getAccessions() != null && data.getAccessions().size() > 0)
				{
					instance.setAttributeValue(ReactomeJavaConstants.secondaryIdentifier, data.getAccessions());
					adaptor.updateInstanceAttribute(instance, ReactomeJavaConstants.secondaryIdentifier);
				}
				break;
			}
			case ReactomeJavaConstants.description:
			{
				if (data.getRecommendedName() != null)
				{
					String alternativeNames = "";
					if (data.getAlternativeNames() != null)
					{
						alternativeNames = data.getAlternativeNames().stream().reduce("", (x,y) -> { return x + " " + y; } );
					}
					String description = data.getRecommendedName() + " " + alternativeNames;
					instance.setAttributeValue(ReactomeJavaConstants.description, description.trim());
					adaptor.updateInstanceAttribute(instance, ReactomeJavaConstants.description);
				}
				break;
			}
			case ReactomeJavaConstants.sequenceLength:
			{
				if (data.getSequenceLength() != null)
				{
					instance.setAttributeValue(ReactomeJavaConstants.sequenceLength, new Integer(data.getSequenceLength()));
					adaptor.updateInstanceAttribute(instance, ReactomeJavaConstants.sequenceLength);
				}
				break;
			}
			case ReactomeJavaConstants.referenceGene:
			{
				break;
			}
			case ReactomeJavaConstants.species:
			{
				String speciesName = data.getScientificName();
				try
				{
					List<GKInstance> dataSpeciesInst = determineDataSpeciesInsts(speciesName);

					Set<Long> speciesDBIDs = new HashSet<>();
					for (GKInstance inst : dataSpeciesInst)
					{
						speciesDBIDs.add(inst.getDBID());
					}
					GKInstance speciesInst = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.species);
					// The list of Species that we got by looking up the name from "data" does not
					// contain the Species DB ID on the current instance.
					// This means we need to update the instance to use the one from the input.
					// does it make sense in the data model for speciesInst to be null?
					if (speciesInst != null && !speciesDBIDs.contains(speciesInst.getDBID()))
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
			case "checksum": //TODO: add "checksum" to ReactomeJavaConstants
			{
				try
				{
					String oldChecksum = (String) instance.getAttributeValue("checksum");
					String newChecksum = data.getSequenceChecksum();
					if (oldChecksum != null && oldChecksum.length() > 0 && newChecksum.equals(oldChecksum))
					{
						updateInstanceChecksum(instance, newChecksum, oldChecksum);
					}
					else
					{
						instance.setAttributeValue("isSequenceChanged", false);
						adaptor.updateInstanceAttribute(instance, "isSequenceChanged");
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

				if (data.getFlattenedGeneNames() != null && data.getFlattenedGeneNames().size() > 0)
				{
					// The first item in the flattened gene names list is the primary gene name.
					instance.setAttributeValue(ReactomeJavaConstants.name, data.getFlattenedGeneNames().get(0));
					adaptor.updateInstanceAttribute(instance, ReactomeJavaConstants.name);
				}
				break;
			}
			case ReactomeJavaConstants.geneName:
			{
				if (data.getFlattenedGeneNames() != null && data.getFlattenedGeneNames().size() > 0)
				{
					// It could happen that there are duplicate gene names that come from the XML file, but we don't want to insert duplicate gene names.
					instance.setAttributeValue(ReactomeJavaConstants.geneName, data.getFlattenedGeneNames().stream().distinct().collect(Collectors.toList()));
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
			case "chain": //TODO: Add "chain" to ReactomeJavaConstants
			{
				if (data.getChains() != null && data.getChains().size() > 0)
				{
					updateChain(instance, data);
				}
				break;
			}
		}
	}


	/**
	 * Produces a list of Species Instances whose name matches a name given as an argument. Also maintains an internal cache of species, keyed by name. Use this method when you want cached species.
	 * @param speciesName - the species name to look up in the cache.
	 * @return A list of Species Instances whose name matches <code>speciesName</code>. It will probably only have 1 item in it.
	 * @throws Exception
	 */
	private List<GKInstance> determineDataSpeciesInsts(String speciesName) throws Exception
	{
		List<GKInstance> dataSpeciesInst;
		if (!speciesCache.containsKey(speciesName))
		{
			// Using a list here because that's what fetchInstanceByAttribute returns but I
			// honestly don't expect more than one result.
			// It would be *very* weird if two different Species objects existed with the
			// same name.
			dataSpeciesInst = new ArrayList<>((Set<GKInstance>) adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Species, ReactomeJavaConstants.name, "=", speciesName));
			speciesCache.put(speciesName, dataSpeciesInst);
			logger.info("Species cache miss on \"{}\"", speciesName);
		}
		else
		{
			dataSpeciesInst = speciesCache.get(speciesName);
		}
		return dataSpeciesInst;
	}

	/**
	 * Updates the "chain" attribute on an instance.
	 * @param instance - the instance to update.
	 * @param data - the UniprotData object on which to base the chain update. 
	 * @throws Exception
	 * @throws InvalidAttributeException
	 * @throws InvalidAttributeValueException
	 */
	private void updateChain(GKInstance instance, UniprotData data) throws Exception, InvalidAttributeException, InvalidAttributeValueException
	{
		List<String> chainStrings = new ArrayList<>();
		for (Chain chain : data.getChains())
		{
			chainStrings.add(chain.toString());
		}
		logChainChanges(new HashSet<>(chainStrings), instance);
		instance.setAttributeValue("chain", chainStrings);
		adaptor.updateInstanceAttribute(instance, "chain");
	}

	/**
	 * Updates an Instance's checksum.
	 * @param instance - the instance to update.
	 * @param data - the UniprotData object which contains the new checksum.
	 * @param oldChecksum - the old checksum.
	 * @throws InvalidAttributeException
	 * @throws InvalidAttributeValueException
	 * @throws Exception
	 */
	private void updateInstanceChecksum(GKInstance instance, String newChecksum, String oldChecksum) throws InvalidAttributeException, InvalidAttributeValueException, Exception
	{
		// The old Perl code prints a warning when the checksum changes.
		sequencesLog.info(
			"Checksum has changed! DB ID: {} \tOld checksum: {} \tNew checksum: {}",
			instance.getDBID(), oldChecksum, newChecksum
		);
		instance.setAttributeValue("checksum", newChecksum);
		adaptor.updateInstanceAttribute(instance, "isSequenceChanged");
		adaptor.updateInstanceAttribute(instance, "checksum");
	}
	/**
	 * Updates the "_chainChangeLog" attribute for an Instance in the database.
	 * @param newChains - A set of new chains to log.
	 * @param instance - The Instance that the chains are associated with.
	 * @throws Exception
	 */
	private void logChainChanges(Set<String> newChains, GKInstance instance) throws Exception
	{
		@SuppressWarnings("unchecked")
		Set<String> oldChains = new HashSet<>((List<String>) instance.getAttributeValuesList("chain"));
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
				logEntry = (logEntry.trim().equals("") ? "" : logEntry + " ; ") + oldChain + " for " + instance.getDBID().longValue() + " removed on " + dateString;
				needsUpdate = true;
			}
		}

		for (String newChain : newChains)
		{
			if (!oldChains.contains(newChain))
			{
				logEntry = (logEntry.trim().equals("") ? "" : logEntry + " ; ") + newChain + " for " + instance.getDBID().longValue() + " added on " + dateString;
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
	/**
	 * Updates a ReferenceGeneProduct: updates the attributes of the ReferenceGeneProduct based on the contents of a UniprotData object.
	 * Also updates or creates Isoforms for the ReferenceGeneProduct.
	 * @param referenceGeneProduct - the ReferenceGeneProduct.
	 * @param data - the UniprotData object.
	 * @param accession - the Uniprot accession.
	 * @throws Exception
	 */
	void updateReferenceGeneProduct(GKInstance referenceGeneProduct, UniprotData data, String accession) throws Exception
	{
		// TODO: add code to check for duplicates.
		updateInstanceWithData(referenceGeneProduct, data);
		updateOrCreateIsoforms(referenceGeneProduct, data.getIsoforms(), accession, data);
	}
	
	static String geneNamesListToString(Collection<Gene> geneNames)
	{
		StringBuilder sb = new StringBuilder();
		for (Gene gene : geneNames)
		{
			for (Name name : gene.getNames())
			{
				sb.append(name.getType()).append(" \"").append(name.getValue()).append("\", ");
			}
		}
		return sb.toString();
	}
	/**
	 * Will check to see if a UniprotData object has any isoforms, and if so it will create new ones or update existing ones in the database.
	 * @param data - The UniprotData object.
	 * @param accession - The Uniprot accession.
	 * @param newRefGeneProduct - This method usually gets called when a new ReferenceGeneProduct has been created. That RGP should be passed in here.
	 * @throws InvalidAttributeException
	 * @throws InvalidAttributeValueException
	 * @throws Exception
	 */
	void addIsoformsIfNecessary(UniprotData data, String accession, GKInstance newRefGeneProduct) throws InvalidAttributeException, InvalidAttributeValueException, Exception
	{
		// add Isoforms if available...
		if (data.getIsoforms() != null)
		{
			for (Isoform isoform : data.getIsoforms())
			{
				createOrUpdateIsoform(accession, newRefGeneProduct, isoform);
			}
		}
	}	

	/**
	 * Creates a new Isoform for a ReferenceGeneProduct, or updates an existing one. If the Isoform's ID contains the Uniprot accession, then a new isoform needs to be created.
	 * Otherwise, there is a mismatch and an existing isoform will get updated.
	 * @param accession - the Uniprot accession.
	 * @param referenceGeneProduct - the ReferenceGeneProduct.
	 * @param isoform - The Isoform to create and add to the RGP. If there is already an Isoform, it may be updated with information in this object.
	 * @throws InvalidAttributeException
	 * @throws InvalidAttributeValueException
	 * @throws Exception
	 */
	void createOrUpdateIsoform(String accession, GKInstance referenceGeneProduct, Isoform isoform) throws InvalidAttributeException, InvalidAttributeValueException, Exception
	{
		String isoformID = isoform.getIsoformID();
		// Check to see if isoformID == accession - it should happen!
		if (isoformID.contains(accession))
		{
			InstanceCreator creator = new InstanceCreator(adaptor, instanceEdit);
			creator.createNewReferenceIsoform(accession, referenceGeneProduct, isoformID);
		}
		else
		{
			// log an error about mismatched isoform ID and accession.
			referenceDNASequenceLog.info("Isoform ID " + isoformID + " does not match Accession " + accession);

			// Update mismatched Isoforms
			updateMismatchedIsoform(isoformID, accession);
		}
	}
	/**
	 * Creates or updates Isoforms for a ReferenceGeneProduct.
	 * If an Isoform's ID (from the XML data file) contains the Uniprot accession, this method will try to get ReferenceIsoforms whose variantIdentifier matches the Isoform's ID.
	 * When an ReferenceIsoform from the database has a variantIdentifier equal to the IsoformID from the XML file, the ReferenceIsoform from the database
	 * gets updated with the content of the UniprotData object. When no ReferenceIsoforms can be found in the database, a new one is created.
	 * If the Uniprot Accession does not contain the Isoform ID as a substring it is considered a mismatch.
	 * @param referenceGeneProduct - The ReferenceGeneProduct
	 * @param isoforms - A list of Isoforms.
	 * @param accession - the Uniprot accession.
	 * @param data - the UniprotData object.
	 * @throws Exception
	 */
	void updateOrCreateIsoforms(GKInstance referenceGeneProduct, List<Isoform> isoforms, String accession, UniprotData data) throws Exception
	{
		if (isoforms != null)
		{
			for (Isoform isoform : isoforms)
			{
				String isoformID = isoform.getIsoformID();

				@SuppressWarnings("unchecked")
				List<GKInstance> refIsoformsFromDB = new ArrayList<>(adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceIsoform, ReactomeJavaConstants.variantIdentifier, "=", isoform.getIsoformID()));
				if (isoformID.contains(accession))
				{
					// Update existing ReferenceIsoforms
					if (refIsoformsFromDB.size() > 0)
					{
						for (GKInstance refIsoformFromDB : refIsoformsFromDB)
						{
							if (((String) refIsoformFromDB.getAttributeValue(ReactomeJavaConstants.variantIdentifier)).equals(isoformID))
							{
								refIsoformFromDB.setAttributeValue(ReactomeJavaConstants.isoformParent, referenceGeneProduct);
								adaptor.updateInstanceAttribute(refIsoformFromDB, ReactomeJavaConstants.isoformParent);
								updateInstanceWithData(refIsoformFromDB, data);
							}
						}
					}
					// Create a shiny *NEW* ReferenceIsoform!
					else
					{
						InstanceCreator creator = new InstanceCreator(adaptor, instanceEdit);
						creator.createNewReferenceIsoform(accession, referenceGeneProduct, isoformID);
					}
				}
				else
				{
					// log a message about mismatches...
					referenceDNASequenceLog.info(
						"Isoform ID {} does not match with Accession {}",
						isoformID, accession
					);
					// Update mismatched Isoforms
					updateMismatchedIsoform(isoformID, accession);
				}
			}
		}
	}
	/**
	 * Updates an existing Isoform where there was a mismatch between the Isoform ID and the Uniprot accession.
	 * The update will set the isoformParent on the Isoform to refer to all Isoforms whose variantIdentifier == isoformID and ReferenceGeneProducts whose identifer == UniprotAccession 
	 * @param isoformID - the Isoform ID
	 * @param accession - the Uniprot Accession.
	 * @throws Exception
	 */
	void updateMismatchedIsoform(String isoformID, String accession) throws Exception
	{
		// Again, we really shouldn't expect more than 1 to be returned, but we still *need* to treat this as a collection, because that's what the API returns.
		@SuppressWarnings("unchecked")
		Set<GKInstance> isoformsFromDB = (Set<GKInstance>) adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceIsoform, ReactomeJavaConstants.variantIdentifier, "=", isoformID);
		List<GKInstance> allParents = new ArrayList<>();
		if (isoformsFromDB != null && !isoformsFromDB.isEmpty())
		{
			for (GKInstance isoformFromDB : isoformsFromDB)
			{
				// Get the current values for "isoformParent" for the isoform.
				GKInstance isoformParents = (GKInstance) isoformFromDB.getAttributeValue(ReactomeJavaConstants.isoformParent);
				if (isoformParents != null /* && !isoformParents.isEmpty() */)
				{
					allParents.add(isoformParents);
				}
				// Get the ReferenceGeneProduct(s) by accession (probably should only return 1, but who knows? I don't think anything enforces RGPs to have unique accessions).
				@SuppressWarnings("unchecked")
				Set<GKInstance> referenceGeneProducts = (Set<GKInstance>) adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct, ReactomeJavaConstants.identifier, "=", accession);
				if (referenceGeneProducts != null && !referenceGeneProducts.isEmpty())
				{
					allParents.addAll(referenceGeneProducts);
				}
				// print a message. This was copied from the Perl implementation, but I do not think it is very clear.
				// TODO: Need a better message!
				referenceDNASequenceLog.info(
					"Mismatched parent: {} ({})\t{}",
					isoformID, isoformFromDB.getDBID(), accession
				);
				// Now correct isoformParent
				isoformFromDB.setAttributeValue(ReactomeJavaConstants.isoformParent, allParents);
				adaptor.updateInstanceAttribute(isoformFromDB, ReactomeJavaConstants.isoformParent);
			}
		}
	}
}
