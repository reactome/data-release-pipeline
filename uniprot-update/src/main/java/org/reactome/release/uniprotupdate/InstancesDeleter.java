package org.reactome.release.uniprotupdate;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.TransactionsNotSupportedException;
import org.gk.schema.GKSchemaAttribute;

/**
 * This class is to be used to delete Uniprot instances.
 * @author sshorser
 *
 */
class InstancesDeleter {
	private static final Logger referenceDNASequenceLog = LogManager.getLogger("referenceDNASequenceLog");
	private static final Logger logger = LogManager.getLogger();

	private Map<String, MySQLAdaptor> adaptorPool = Collections.synchronizedMap(new HashMap<>());

	private MySQLAdaptor getAdaptorForThread(MySQLAdaptor baseAdaptor, String threadIdentifier) throws SQLException {
		MySQLAdaptor adaptor;
		if (adaptorPool.containsKey(threadIdentifier)) {
			adaptor = adaptorPool.get(threadIdentifier);
		} else {
			MySQLAdaptor adaptorForPool = new MySQLAdaptor(
				baseAdaptor.getDBHost(),
				baseAdaptor.getDBName(),
				baseAdaptor.getDBUser(),
				baseAdaptor.getDBPwd(),
				baseAdaptor.getDBPort()
			);
			adaptorPool.put(threadIdentifier, adaptorForPool);
			adaptor = adaptorForPool;
		}
		return adaptor;
	}

	private void cleanAdaptorPool() throws Exception {
		for (String k : adaptorPool.keySet()) {
			adaptorPool.get(k).cleanUp();
		}
		this.adaptorPool.clear();
	}

	/**
	 * Delete obsolete instances. An instance is considered "obsolete" if it has no referrers.
	 * @param adaptor - the adaptor to use.
	 * @param pathToUnreviewedUniprotIDsFile - path to the "Unreviewed UniprotIDs file" -
	 * if a Uniprot identifier is not found in this file, the instance associated with the identifier is
	 * a potential candidate for deletion (depending on referrer counts)
	 * @throws Exception
	 */
	void deleteObsoleteInstances(MySQLAdaptor adaptor, String pathToUnreviewedUniprotIDsFile) throws Exception {
		logger.info("Preparing to delete obsolete instances...");
		@SuppressWarnings("unchecked")
		Collection<GKInstance> allReferenceGeneProducts = (Collection<GKInstance>) adaptor.fetchInstancesByClass(
			ReactomeJavaConstants.ReferenceGeneProduct
		);
		logger.info("{} ReferenceGeneProducts need to be checked.", allReferenceGeneProducts.size());
		Map<String, GKInstance> referenceGeneProductMap = new HashMap<>(allReferenceGeneProducts.size());
		for (GKInstance referenceGeneProduct : allReferenceGeneProducts) {
			String identifier = (String) referenceGeneProduct.getAttributeValue(ReactomeJavaConstants.identifier);
			if (identifier != null) {
				referenceGeneProductMap.put(identifier, referenceGeneProduct);
			}
		}
		logger.info("{} ReferenceGeneProducts in map.", referenceGeneProductMap.size());

		Set<String> identifiersInFileAndDB = findIdentifiersInFileAndDB(
			pathToUnreviewedUniprotIDsFile, referenceGeneProductMap
		);
		logger.info(
			"{} identifiers that are in the database AND in the unreviewed Uniprot identifier file.",
			identifiersInFileAndDB.size()
		);
		// Now that we know which identifiers *are* in the list of unreviewed Uniprot identifiers,
		// we need to look at all ReferenceGeneProducts *not* in that list. Might need to delete some of them.
		List<String> identifiersToCheck = referenceGeneProductMap.keySet().parallelStream()
			.filter(identifier -> !identifiersInFileAndDB.contains(identifier))
			.collect(Collectors.toList());
		logger.info("{} identifiers need to be checked for referrer count.", identifiersToCheck.size());
		List<GKInstance> identifiersToDelete = findIdentifiersToDelete(
			adaptor, referenceGeneProductMap, identifiersToCheck
		);
		this.cleanAdaptorPool();
		// Now do the actual deletions
		logger.info("{} ReferenceGeneProducts will be deleted.", identifiersToDelete.size());
		int deletedCount = deleteInstances(adaptor, identifiersToDelete);
		logger.info("{} instances have been deleted.", deletedCount);
		logger.info("Finished deleting obsolete ReferenceGeneProducts with no referrers.");
		// TODO: The number of items in the file will be much larger than the number of ReferenceGeneProducts
		//  (120243849 vs 115769, at the time of writing 2018-07-31).
		// So... instead of trying to load the whole file into memory, convert the ReferenceGeneProducts into a Map
		// (keyed by identifier) and check the map for each line read - then you don't have to store the whole file
		// in memory, just the current line!  Will probably be faster. The file has 120243849 lines and Java is slowing
		// down when trying to load each line into a Set (TreeSet or HashSet - they both perform poorly).
		// TODO: Idea for parallelization: instead of deleting objects in this loop, process these items in parallel
		//  and add the DB IDs of things to delete to a thread-safe list,
		// then go through that list (serially) and delete things.
		// TODO: Add a progress meter/counter for this loop.
	}

	/**
	 * Finds identifiers to delete.
	 * An instance will be added to the list of "Instances to delete" if it has 0 referrers.
	 * @param adaptor - the adaptor to use.
	 * @param referenceGeneProductMap - a Map of ReferenceGeneProducts, keyed by identifier.
	 * @param identifiersToCheck - a list of identifiers to check
	 * @return a list of Instances that can be deleted.
	 */
	private List<GKInstance> findIdentifiersToDelete(
		MySQLAdaptor adaptor, Map<String, GKInstance> referenceGeneProductMap, List<String> identifiersToCheck
	) {
		List<GKInstance> identifiersToDelete = new ArrayList<>();
		// TODO: this loop is slow. Multithread it somehow.
		//for (String identifier : identifiersToCheck)
		// Check each identifier: If it has 0 referrers, no variantIdentifier, and is not in the
		// unreviewed Uniprot IDs list, it can be deleted, so it will be added to identifiersToDelete.
		identifiersToCheck.parallelStream().forEach( identifier -> {
			try {
				MySQLAdaptor tmpAdaptor = this.getAdaptorForThread(adaptor, Thread.currentThread().getName());
				GKInstance referenceGeneProduct = referenceGeneProductMap.get(identifier);
				referenceGeneProduct.setDbAdaptor(tmpAdaptor);
				// ReferenceGeneProducts should all have the same referring attributes, so this
				// collection should only be populated once.
	//			if (referringAttributes == null)
	//			{
	//				referringAttributes =
	//					(Collection<GKSchemaAttribute>) referenceGeneProduct.getSchemClass().getReferers();
	//			}
				int referrerCount = getReferrerCount(referenceGeneProduct);
				if (referrerCount == 0) {
					referenceDNASequenceLog.info(
						"ReferenceGeneProduct " + referenceGeneProduct.toString() + " has 0 referrers, " +
						"no variantIdentifier, and is not in the unreviewed Uniprot IDs list, so it will be deleted."
					);
					identifiersToDelete.add(referenceGeneProduct);
				}
				referenceGeneProduct.setDbAdaptor(adaptor);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		return identifiersToDelete;
	}

	/**
	 * Finds identifiers that are in the "Unreviewed Uniprot IDs" file AND and in the database.
	 * @param pathToUnreviewedUniprotIDsFile - the path to the file.
	 * @param referenceGeneProductMap - the Map of ReferenceGeneProducts, keyed by identifier.
	 * @return
	 * @throws FileNotFoundException
	 */
	private Set<String> findIdentifiersInFileAndDB(String pathToUnreviewedUniprotIDsFile,
		Map<String, GKInstance> referenceGeneProductMap)
			throws FileNotFoundException {

		Set<String> identifiersInFileAndDB = new HashSet<>();
		logger.info("Loading file: {}", pathToUnreviewedUniprotIDsFile);
		FileInputStream fis = new FileInputStream(pathToUnreviewedUniprotIDsFile);
		BufferedInputStream bis = new BufferedInputStream(fis);
		try (Scanner scanner = new Scanner(bis)) {
			while (scanner.hasNextLine()) {
				String identifierFromFile = scanner.nextLine().trim();
				if (referenceGeneProductMap.containsKey(identifierFromFile)) {
					identifiersInFileAndDB.add(identifierFromFile);
				}
			}
		}
		return identifiersInFileAndDB;
	}

	/**
	 * Gets the number of referrers an instance has.
	 * @param referenceGeneProduct - the ReferenceGeneProduct to check.
	 * @return The number of instances that refer to the given object.
	 * @throws Exception
	 */
	private int getReferrerCount(GKInstance referenceGeneProduct) throws Exception {
		@SuppressWarnings("unchecked")
		Collection<GKSchemaAttribute> referringAttributes =
			(Collection<GKSchemaAttribute>) referenceGeneProduct.getSchemClass().getReferers();

		int referrerCount = 0;
		for (GKSchemaAttribute referringAttribute : referringAttributes) {
			@SuppressWarnings("unchecked")
			Collection<GKInstance> referrers = referenceGeneProduct.getReferers(referringAttribute);
			referrerCount += referrers.size();
		}
		return referrerCount;
	}

	/**
	 * Deletes instances.
	 * @param adaptor - the database adaptor to use.
	 * @param identifiersToDelete - the list of identifiers that should be deleted.
	 * @return the number of items that were deleted.
	 * @throws SQLException
	 * @throws TransactionsNotSupportedException
	 */
	private int deleteInstances(MySQLAdaptor adaptor, List<GKInstance> identifiersToDelete)
		throws SQLException, TransactionsNotSupportedException {

		int deletedCount = 0;
		int txnDeleteCount = 0;
		adaptor.startTransaction();
		for (GKInstance referenceGeneProuductToDelete : identifiersToDelete) {
			try {
				// Deletion is SLOOOOOW... maybe have multiple threads to do deletion?
				// ...tried that. It made things worse. :(
				adaptor.deleteInstance(referenceGeneProuductToDelete);
				if (txnDeleteCount == 1000) {
					adaptor.commit();
					txnDeleteCount = 0;
				} else {
					txnDeleteCount++;
				}
				int count = deletedCount;
				if (count % 1000 == 0) {
					logger.info("{} instances have been deleted.", count);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		adaptor.commit();
		return deletedCount;
	}
}
