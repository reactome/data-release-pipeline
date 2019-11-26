package org.reactome.release.uniprotupdate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.TransactionsNotSupportedException;
import org.reactome.release.common.ReleaseStep;
import org.reactome.release.common.database.InstanceEditUtils;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

import java.sql.SQLException;
import java.util.*;

/**
 *
 * @author sshorser, jweiser
 */
public class UniprotUpdateStep extends ReleaseStep {
	private static final String SAVEPOINT_NAME = "PRE_UNIPROT_UPDATE";

	private static final Logger logger = LogManager.getLogger();

	@Override
	public void executeStep(Properties props) throws Exception {
		this.setTestModeFromProperties(props);

		// Extract property values for MySQL adaptor
		MySQLAdaptor adaptor = getMySQLAdaptorFromProperties(props);

		// Start UniProt Update database transaction
		startTransaction(adaptor, SAVEPOINT_NAME);

		// Create instance edit
		long personID = Long.parseLong(props.getProperty("person.id"));
		String creatorName = this.getClass().getName();
		GKInstance instanceEdit = InstanceEditUtils.createInstanceEdit(adaptor, personID, creatorName);

		// Extract data from UniProt XML
		String pathToUniprotFile = props.getProperty("pathToUniprotFile");
		List<UniprotData> uniprotEntries = ProcessUniprotXML.getDataFromUniprotFile(pathToUniprotFile, debugXML(props));

		UniprotUpdater updater = new UniprotUpdater(adaptor, uniprotEntries);
		// Update UniProt ReferenceGeneProduct instances
		updater.updateUniprotInstances(instanceEdit);

		// Commit changes so far - deletion will be multithreaded, so each adaptor will need its own transaction.
		adaptor.commit();

		// Delete obsolete instances
		String pathToUnreviewedUniprotIDsFile = props.getProperty("pathToUnreviewedUniprotIDsFile");
		InstancesDeleter deleter = new InstancesDeleter();
		deleter.deleteObsoleteInstances(adaptor, pathToUnreviewedUniprotIDsFile);

		if (testMode) {
			rollbackTransaction(adaptor, SAVEPOINT_NAME);
		} else {
			commitTransaction(adaptor);
		}
		logger.info("Done.");
	}

	/**
	 * Gets a map of instances (keyed by identifier values) for a specified Reactome class name and restricted
	 * to a specific reference database.
	 * @param reactomeClassName - the Reactome "type" to which the instances will be constrained.
	 * @param refDBName - the name of the reference database to which the instances will be constrained.
	 * @param adaptor - the database adaptor to use.
	 * @return Map of String identifier to a GKInstance object containing the identifier
	 * @throws Exception Thrown if unable to fetch instances by the provided reactomeClassName or if there is a problem
	 * calling the cleanUp method on any MySQLAdaptor objects in the adaptorPool
	 */
	private Map<String, GKInstance> getIdentifierMappedCollectionOfType(
		String reactomeClassName, String refDBName, MySQLAdaptor adaptor
	) throws Exception {
		@SuppressWarnings("unchecked")
		Collection<GKInstance> instances = (Collection<GKInstance>) adaptor.fetchInstancesByClass(reactomeClassName);
		Map<String, GKInstance> instanceMap = Collections.synchronizedMap(new HashMap<>(instances.size()));
		Map<String, MySQLAdaptor> adaptorPool = Collections.synchronizedMap(new HashMap<>());

		instances.parallelStream().forEach( instance -> {
			MySQLAdaptor tmpAdaptor = getTemporaryAdaptor(instance, adaptorPool);
			if (tmpAdaptor != null) {
				instance.setDbAdaptor(tmpAdaptor);
			}

			String identifier = getIdentifierAttributeValue(instance);
			// Specify a ReferenceDatabase name that the instances should be constrained by (no constraint if no name is
			// specified).
			if (!identifier.isEmpty() && instanceHasAllowedRefDB(instance, refDBName)) {
				instanceMap.put(identifier, instance);
			}

			// set back to the main adaptor
			instance.setDbAdaptor(adaptor);
		});

		// Clean up other adaptors.
		for (MySQLAdaptor dba : adaptorPool.values()) {
			dba.cleanUp();
		}

		logger.info("{}s {} mapped by Identifier.", reactomeClassName, instanceMap.size());

		return instanceMap;
	}

	private boolean debugXML(Properties props) {
		return Boolean.parseBoolean(props.getProperty("debugXML", "false"));
	}

	private MySQLAdaptor getTemporaryAdaptor(GKInstance instance, Map<String, MySQLAdaptor> adaptorPool) {
		final String CURRENT_THREAD_NAME = Thread.currentThread().getName();

		return adaptorPool.computeIfAbsent(CURRENT_THREAD_NAME, currentThread -> {
			try {
				return cloneInstanceDBAdaptor(instance);
			} catch (SQLException e) {
				logger.error("Unable to create a temporary MySQLAdaptor for " + instance.getExtendedDisplayName(), e);
				return null;
			}
		});
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

	private boolean instanceHasAllowedRefDB(GKInstance instance, String refDBName)  {
		// No constraint if no reference database name is specified
		if (refDBName == null || refDBName.isEmpty()) {
			return true;
		}

		try {
			GKInstance refDB = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
			return (refDB != null && refDBName.equals(refDB.getAttributeValue(ReactomeJavaConstants.name)));
		} catch (Exception e) {
			String errorMessage = "Unable to get reference database (or its name) for " +
				instance.getExtendedDisplayName();
			logger.fatal(errorMessage);
			throw new RuntimeException(errorMessage, e);
		}
	}

	private String getIdentifierAttributeValue(GKInstance instance) {
		final String EMPTY_STRING = "";
		try {
			String identifier = (String) instance.getAttributeValue(ReactomeJavaConstants.identifier);
			return identifier != null && !identifier.isEmpty() ? identifier : EMPTY_STRING;
		} catch (Exception e) {
			logger.error("Unable to get identifier attribute value for " + instance.getExtendedDisplayName(), e);
			return EMPTY_STRING;
		}
	}

	private void startTransaction(MySQLAdaptor adaptor, String savePoint)
		throws TransactionsNotSupportedException, SQLException {
		logger.info("Starting transaction with SAVEPOINT name " + savePoint);
		adaptor.executeQuery("SAVEPOINT " + savePoint, null);
		adaptor.startTransaction();
	}

	private void rollbackTransaction(MySQLAdaptor adaptor, String savePoint) throws SQLException {
		logger.info("Rolling back transaction to SAVEPOINT " + savePoint);
		adaptor.executeQuery("ROLLBACK TO " + SAVEPOINT_NAME , null);
	}

	private void commitTransaction(MySQLAdaptor adaptor) throws SQLException {
		logger.info("Committing transaction");
		adaptor.commit();
	}
}
