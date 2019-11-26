package org.reactome.release.uniprotupdate;

import java.sql.SQLException;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.reactome.release.uniprotupdate.dataschema.GeneName;
import org.reactome.release.uniprotupdate.dataschema.Gene;
import org.reactome.release.uniprotupdate.dataschema.Isoform;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;
import org.reactome.release.uniprotupdate.instanceupdate.AttributeUpdater;
import org.reactome.release.uniprotupdate.instanceupdate.AttributeUpdaterFactory;

public abstract class AbstractDataProcessor {
	protected MySQLAdaptor adaptor;
	protected GKInstance instanceEdit;

	protected static final Logger logger = LogManager.getLogger();
	private static final Logger sequencesLog = LogManager.getLogger("sequencesLog");
	protected static final Logger uniprotRecordsLog = LogManager.getLogger("uniprotRecordsLog");
	protected static final Logger referenceDNASequenceLog = LogManager.getLogger("referenceDNASequenceLog");

	private Map<String, GKInstance> referenceGeneProducts;
	private Map<String, GKInstance> referenceDNASequences;
	private Map<String, GKInstance> referenceIsoforms;

	public AbstractDataProcessor(MySQLAdaptor adaptor, GKInstance instanceEdit) {
		this.adaptor = adaptor;
		this.instanceEdit = instanceEdit;
	}

	abstract void processData(UniprotData data)
		throws InvalidAttributeException, InvalidAttributeValueException, Exception;

	private Map<String, GKInstance> getReferenceIsoforms(MySQLAdaptor adaptor) throws Exception {
		if (this.referenceIsoforms == null) {
			this.referenceIsoforms = getIdentifierMappedCollectionOfType(
				ReactomeJavaConstants.ReferenceIsoform, "UniProt", adaptor
			);
		}

		return this.referenceIsoforms;
	}

	Map<String, GKInstance> getReferenceDNASequences(MySQLAdaptor adaptor) throws Exception {
		if (this.referenceDNASequences == null) {
			this.referenceDNASequences = getIdentifierMappedCollectionOfType(
				ReactomeJavaConstants.ReferenceDNASequence, adaptor
			);
		}

		return this.referenceDNASequences;
	}

	Map<String, GKInstance> getReferenceGeneProducts(MySQLAdaptor adaptor)
			throws Exception, InvalidAttributeException {
		if (this.referenceGeneProducts == null) {
			this.referenceGeneProducts = getIdentifierMappedCollectionOfType(
				ReactomeJavaConstants.ReferenceGeneProduct, "UniProt", adaptor
			);
		}

		return this.referenceGeneProducts;
	}

	/**
	 * Gets a map of instances, keyed by identifier.
	 * @param reactomeClassName - the Reactome "type" to which the instances will be constrained.
	 * @param refDBName - the name of the reference database to which the instances will be constrained.
	 * @param adaptor - the database adaptor to use.
	 * @return
	 * @throws Exception
	 * @throws InvalidAttributeException
	 */
	Map<String, GKInstance> getIdentifierMappedCollectionOfType(String reactomeClassName, String refDBName, MySQLAdaptor adaptor)
		throws Exception, InvalidAttributeException {

		@SuppressWarnings("unchecked")
		Collection<GKInstance> instances = (Collection<GKInstance>) adaptor.fetchInstancesByClass(reactomeClassName);
		Map<String, GKInstance> instanceMap = Collections.synchronizedMap(new HashMap<>(instances.size()));
		Map<String, MySQLAdaptor> adaptorPool = Collections.synchronizedMap(new HashMap<>());

		instances.parallelStream().forEach( instance -> {
			try {
				MySQLAdaptor tmpAdaptor = adaptorPool.get(Thread.currentThread().getName());;
				if (tmpAdaptor == null) {
					tmpAdaptor = cloneInstanceDBAdaptor(instance);
					adaptorPool.put(Thread.currentThread().getName(), tmpAdaptor);
				}
				instance.setDbAdaptor(tmpAdaptor);

				String identifier;
				try {
					identifier = (String) instance.getAttributeValue(ReactomeJavaConstants.identifier);
					if (identifier !=null && identifier.length() > 0) {
						// fast-load the attributes now so accessing them later will be faster.
						// tmpAdaptor.fastLoadInstanceAttributeValues(instance);
						// Specify a ReferenceDatabase name that the instances should be constrained by.
						if (instanceHasAllowedRefDB(instance, refDBName)) {
							instanceMap.put(identifier, instance);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				// set back to the main adaptor
				instance.setDbAdaptor(adaptor);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		// Clean up other adaptors.
		for (String k : adaptorPool.keySet()) {
			adaptorPool.get(k).cleanUp();
		}

		logger.info("{}s {} mapped by Identifier.", reactomeClassName, instanceMap.size());

		return instanceMap;
	}

	/**
	 * Gets a map of instances (keyed by identifier values) for a specified Reactome class name
	 * @param reactomeClassName - the Reactome "type" to which the instances will be constrained.
	 * @param adaptor - the database adaptor to use.
	 * @return Map of String identifier to a GKInstance object containing the identifier
	 * @throws Exception Thrown if unable to fetch instances by the provided reactomeClassName or if there is a problem
	 * calling the cleanUp method on any MySQLAdaptor objects in the adaptorPool
	 */
	private Map<String, GKInstance> getIdentifierMappedCollectionOfType(
		String reactomeClassName, MySQLAdaptor adaptor
	) throws Exception {
		final String ANY_REFERENCE_DATABASE_NAME = "";
		return getIdentifierMappedCollectionOfType(reactomeClassName, ANY_REFERENCE_DATABASE_NAME, adaptor);
	}

	private MySQLAdaptor cloneInstanceDBAdaptor(GKInstance instance) throws SQLException {
		MySQLAdaptor instanceAdaptor = (MySQLAdaptor) instance.getDbAdaptor();

		return new MySQLAdaptor(
			instanceAdaptor.getDBHost(),
			instanceAdaptor.getDBName(),
			instanceAdaptor.getDBUser(),
			instanceAdaptor.getDBPwd(),
			instanceAdaptor.getDBPort()
		);
	}

	private boolean instanceHasAllowedRefDB(GKInstance instance, String refDBName)
		throws Exception, InvalidAttributeException {

		// No constraint if no reference database name is specified
		if (refDBName == null) {
			return true;
		}

		GKInstance refDB = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
		return (refDB != null && refDBName.equals(refDB.getAttributeValue(ReactomeJavaConstants.name)));

	}

	/**
	 * This function will update attributes on an instance, based on the content of a UniprotData object.
	 * @param instance - The instance to try updating.
	 * @param data - The UniprotData object that may contain new data for the instance.
	 * @param attributes - A list of attributes to update.
	 * If you do not pass a list, the default list (everything!) will be attempted:
	 * <ul>
	 *     <li>secondaryIdentifier</li><li>description</li><li>sequenceLength</li>
	 *     <li>species</li><li>checksum</li><li>name</li><li>geneName</li>
	 *     <li>comment</li><li>keyword</li><li>chain</li>
	 * </ul>
	 * If you pass in your own list of attributes, <b>make sure they are valid</b> for <code>instance</code>!
	 * This function does <em>not</em> check attribute validity.
	 */
	void updateInstanceWithData(GKInstance instance, UniprotData data, String... attributes) {
		// Set the default list if the user does not specify anything.
		String[] attributesToUse = hasValues(attributes) ? attributes : getDefaultAttributes();
		for (String attribute : attributesToUse) {
			// The old Perl code actually prints messages every time the old data differs from the new data.
			// Is that really necessary?
			AttributeUpdater attributeUpdater = AttributeUpdaterFactory.getFactory().createAttributeUpdater(attribute);
			attributeUpdater.updateAttribute(instance, data);
		}
	}

	private boolean hasValues(String ...stringArray) {
		return stringArray != null && stringArray.length > 0;
	}

	private String[] getDefaultAttributes() {
		return new String[] {
			ReactomeJavaConstants.secondaryIdentifier,
			ReactomeJavaConstants.description,
			ReactomeJavaConstants.sequenceLength,
			ReactomeJavaConstants.species,
			"checksum",
			ReactomeJavaConstants.name,
			ReactomeJavaConstants.geneName,
			ReactomeJavaConstants.comment,
			ReactomeJavaConstants.keyword,
			"chain"
		};
	}

	/**
	 * Updates a ReferenceGeneProduct: updates the attributes of the ReferenceGeneProduct based
	 * on the contents of a UniprotData object.
	 * Also updates or creates Isoforms for the ReferenceGeneProduct.
	 * @param referenceGeneProduct - the ReferenceGeneProduct.
	 * @param data - the UniprotData object.
	 * @throws Exception
	 */
	void updateReferenceGeneProduct(GKInstance referenceGeneProduct, UniprotData data) throws Exception	{
		// TODO: add code to check for duplicates.
		updateInstanceWithData(referenceGeneProduct, data);

		IsoformProcessor isoformProcessor = new IsoformProcessor();
		isoformProcessor.updateOrCreateIsoforms(referenceGeneProduct, data);
	}

	static String geneNamesListToString(Collection<Gene> geneNames) {
		StringBuilder sb = new StringBuilder();
		for (Gene gene : geneNames) {
			for (GeneName name : gene.getNames()) {
				sb
					.append(name.getType())
					.append(" \"")
					.append(name.getValue())
					.append("\", ");
			}
		}
		return sb.toString();
	}

	class IsoformProcessor {
		/**
		 * Creates or updates Isoforms for a ReferenceGeneProduct.
		 *
		 * If an Isoform's ID (from the XML data file) contains the Uniprot accession, this method will try to get
		 * ReferenceIsoforms whose variantIdentifier matches the Isoform's ID.
		 *
		 * When an ReferenceIsoform from the database has a variantIdentifier equal to the IsoformID from the XML file,
		 * the ReferenceIsoform from the database gets updated with the content of the UniprotData object.
		 *
		 * When no ReferenceIsoforms can be found in the database, a new one is created.
		 *
		 * If the Uniprot Accession does not contain the Isoform ID as a substring it is considered a mismatch.
		 * @param referenceGeneProduct - The ReferenceGeneProduct
		 * @param uniprotEntry - the UniprotData object.
		 * @throws Exception
		 */
		void updateOrCreateIsoforms(GKInstance referenceGeneProduct, UniprotData uniprotEntry) throws Exception {
			for (String isoformID : uniprotEntry.getIsoforms()) {
				@SuppressWarnings("unchecked")
				List<GKInstance> refIsoformsFromDB = new ArrayList<>(adaptor.fetchInstanceByAttribute(
					ReactomeJavaConstants.ReferenceIsoform,
					ReactomeJavaConstants.variantIdentifier,
					"=",
					isoformID
				));
				String primaryAccession = uniprotEntry.getPrimaryAccession();
				if (isoformID.contains(primaryAccession)) {
					if (refIsoformsFromDB.isEmpty()) {
						createIsoform(primaryAccession, referenceGeneProduct, isoformID);
					} else {
						updateIsoform(referenceGeneProduct, refIsoformsFromDB, isoformID, uniprotEntry);
					}
				} else {
					// log a message about mismatches...
					referenceDNASequenceLog.info(
						"Isoform ID {} does not match with Accession {}",
						isoformID, primaryAccession
					);
					// Update mismatched Isoforms
					updateMismatchedIsoform(refIsoformsFromDB, isoformID, primaryAccession);
				}
			}
		}

		private void updateIsoform(GKInstance referenceGeneProduct, List<GKInstance> refIsoformsFromDB, String isoformID, UniprotData data)
			throws Exception {
			// Update existing ReferenceIsoforms
			for (GKInstance refIsoformFromDB : refIsoformsFromDB) {
				if (refIsoformFromDB.getAttributeValue(ReactomeJavaConstants.variantIdentifier).equals(isoformID)) {
					refIsoformFromDB.setAttributeValue(ReactomeJavaConstants.isoformParent, referenceGeneProduct);
					adaptor.updateInstanceAttribute(refIsoformFromDB, ReactomeJavaConstants.isoformParent);
					AbstractDataProcessor.this.updateInstanceWithData(refIsoformFromDB, data);
				}
			}
		}

		private void createIsoform(String accession, GKInstance referenceGeneProduct, String isoformID)
			throws Exception {
			InstanceCreator creator = new InstanceCreator(adaptor, instanceEdit);
			creator.createNewReferenceIsoform(accession, referenceGeneProduct, isoformID);
		}

		/**
		 * Updates an existing Isoform where there was a mismatch between the Isoform ID and the Uniprot accession.
		 * The update will set the isoformParent on the Isoform to refer to
		 * all Isoforms whose variantIdentifier == isoformID and ReferenceGeneProducts whose identifer == UniprotAccession
		 * @param isoformID - the Isoform ID
		 * @param accession - the Uniprot Accession.
		 * @throws Exception
		 */
		private void updateMismatchedIsoform(List<GKInstance> isoformsFromDB, String isoformID, String accession)
			throws Exception {
			List<GKInstance> allParents = new ArrayList<>();
			for (GKInstance isoformFromDB : isoformsFromDB) {
				// Get the current values for "isoformParent" for the isoform.
				GKInstance isoformParent =
					(GKInstance) isoformFromDB.getAttributeValue(ReactomeJavaConstants.isoformParent);
				if (isoformParent != null) {
					allParents.add(isoformParent);
				}

				// Get the ReferenceGeneProduct(s) by accession (probably should only return 1, but who knows?
				// I don't think anything enforces RGPs to have unique accessions).
				@SuppressWarnings("unchecked")
				Set<GKInstance> referenceGeneProducts = (Set<GKInstance>) adaptor.fetchInstanceByAttribute(
					ReactomeJavaConstants.ReferenceGeneProduct, ReactomeJavaConstants.identifier, "=", accession
				);
				if (referenceGeneProducts != null && !referenceGeneProducts.isEmpty()) {
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
