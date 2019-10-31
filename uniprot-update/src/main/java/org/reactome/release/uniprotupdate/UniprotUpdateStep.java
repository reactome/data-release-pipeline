package org.reactome.release.uniprotupdate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.TransactionsNotSupportedException;
import org.gk.schema.InvalidAttributeException;
import org.reactome.release.common.ReleaseStep;
import org.reactome.release.common.database.InstanceEditUtils;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

import java.sql.SQLException;
import java.util.*;

/**
 *
 * @author sshorser
 *
 */
public class UniprotUpdateStep extends ReleaseStep {
	private static final String SAVEPOINT_NAME = "PRE_UNIPROT_UPDATE";

	private static final Logger logger = LogManager.getLogger();

	@Override
	public void executeStep(Properties props) throws Exception {
		this.setTestModeFromProperties(props);

		// Extract property values
		String pathToUniprotFile = props.getProperty("pathToUniprotFile");
		String pathToUnreviewedUniprotIDsFile = props.getProperty("pathToUnreviewedUniprotIDsFile");
		MySQLAdaptor adaptor = getMySQLAdaptorFromProperties(props);
		long personID = Long.parseLong(props.getProperty("person.id"));

		// Start UniProt Update database transaction
		startTransaction(adaptor, SAVEPOINT_NAME);

		// Create instance edit
		String creatorName = this.getClass().getName();
		GKInstance instanceEdit = InstanceEditUtils.createInstanceEdit(adaptor, personID, creatorName);

		UniprotUpdater updater = new UniprotUpdater();
		// Extract data from UniProt XML
		List<UniprotData> uniprotData = ProcessUniprotXML.getDataFromUniprotFile(pathToUniprotFile, debugXML(props));

		// Update UniProt ReferenceGeneProduct instances
		updater.updateUniprotInstances(
			adaptor,
			uniprotData,
			getReferenceDNASequences(adaptor),
			getReferenceGeneProducts(adaptor),
			getReferenceIsoforms(adaptor),
			instanceEdit
		);

		// Commit changes so far - deletion will be multithreaded, so each adaptor will need its own transaction.
		adaptor.commit();

		// Delete obsolete instances
		InstancesDeleter deleter = new InstancesDeleter();
		deleter.deleteObsoleteInstances(adaptor, pathToUnreviewedUniprotIDsFile);

		if (testMode) {
			rollbackTransaction(adaptor, SAVEPOINT_NAME);
		} else {
			commitTransaction(adaptor);
		}
		logger.info("Done.");
	}

	private Map<String, GKInstance> getReferenceDNASequences(MySQLAdaptor adaptor) throws Exception {
		return getIdentifierMappedCollectionOfType(adaptor, ReactomeJavaConstants.ReferenceDNASequence, null);
	}

	private Map<String, GKInstance> getReferenceGeneProducts(MySQLAdaptor adaptor) throws Exception {
		return getIdentifierMappedCollectionOfType(adaptor, ReactomeJavaConstants.ReferenceGeneProduct, "UniProt");
	}

	private Map<String, GKInstance> getReferenceIsoforms(MySQLAdaptor adaptor) throws Exception {
		return getIdentifierMappedCollectionOfType(adaptor, ReactomeJavaConstants.ReferenceIsoform, "UniProt");
	}

	/**
	 * Gets a map of instances, keyed by identifier.
	 * @param adaptor - the database adaptor to use.
	 * @param reactomeClassName - the Reactome "type" to which the instances will be constrained.
	 * @param refDBName - the name of the reference database to which the instances will be constrained.
	 * @return
	 * @throws Exception
	 * @throws InvalidAttributeException
	 */
	private Map<String, GKInstance> getIdentifierMappedCollectionOfType(
		MySQLAdaptor adaptor, String reactomeClassName, String refDBName)
			throws Exception, InvalidAttributeException
	{
		@SuppressWarnings("unchecked")
		Collection<GKInstance> instances = (Collection<GKInstance>)adaptor.fetchInstancesByClass(reactomeClassName);
		Map<String, GKInstance> instanceMap = Collections.synchronizedMap(new HashMap<>(instances.size()));
		//for (GKInstance instance : instances)
		Map<String, MySQLAdaptor> adaptorPool = Collections.synchronizedMap(new HashMap<>());

		instances.parallelStream().forEach( instance ->
		{
			try
			{
				MySQLAdaptor tmpAdaptor = adaptorPool.get(Thread.currentThread().getName());;
				if (tmpAdaptor == null)
				{
					tmpAdaptor = cloneInstanceDBAdaptor(instance);
					adaptorPool.put(Thread.currentThread().getName(), tmpAdaptor);
				}
				instance.setDbAdaptor(tmpAdaptor);

				String identifier;
				try
				{
					identifier = (String) instance.getAttributeValue(ReactomeJavaConstants.identifier);
					if (identifier !=null && identifier.length() > 0)
					{
						// fast-load the attributes now so accessing them later will be faster.
						// tmpAdaptor.fastLoadInstanceAttributeValues(instance);
						// Specify a ReferenceDatabase name that the instances should be constrained by.
						if (instanceHasAllowedRefDB(instance, refDBName)) {
							instanceMap.put(identifier, instance);
						}
					}
				} catch (Exception e)
				{
					e.printStackTrace();
				}
				// set back to the main adaptor
				instance.setDbAdaptor(adaptor);
			}
			catch (SQLException e)
			{
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

	private boolean debugXML(Properties props) {
		return Boolean.parseBoolean(props.getProperty("debugXML", "false"));
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

	private boolean instanceHasAllowedRefDB(GKInstance instance, String refDBName) throws Exception {
		// No constraint if no reference database name is specified
		if (refDBName == null) {
			return true;
		}

		GKInstance refDB = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
		return (refDB != null && refDBName.equals(refDB.getAttributeValue(ReactomeJavaConstants.name)));
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
