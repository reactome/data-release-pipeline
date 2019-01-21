package org.reactome.release.uniprotupdate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
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
public class UniprotUpdateStep extends ReleaseStep
{

	private static final String SAVEPOINT_NAME = "PRE_UNIPROT_UPDATE";

	private static final Logger logger = LogManager.getLogger();

	@Override
	public void executeStep(Properties props) throws Exception
	{
		String pathToUniprotFile = props.getProperty("pathToUniprotFile");
		String pathToUnreviewedUniprotIDsFile = props.getProperty("pathToUnreviewedUniprotIDsFile");
		MySQLAdaptor adaptor = getMySQLAdaptorFromProperties(props);
		String personID = props.getProperty("person.id");

		this.loadTestModeFromProperties(props);

		adaptor.executeQuery("SAVEPOINT " + SAVEPOINT_NAME , null);
		adaptor.startTransaction();

		String creatorName = this.getClass().getName();
		GKInstance instanceEdit = InstanceEditUtils.createInstanceEdit(adaptor, Long.valueOf(personID), creatorName);
		UniprotUpdater updater = new UniprotUpdater();
		List<UniprotData> uniprotData = ProcessUniprotXML.getDataFromUniprotFile(pathToUniprotFile, debugXML(props));
		updater.updateUniprotInstances(
			adaptor,
			uniprotData,
			getReferenceDNASequences(adaptor),
			getReferenceGeneProducts(adaptor),
			getReferenceIsoforms(adaptor),
			instanceEdit
		);
		// commit changes so far - deletion will be multithreaded, so each adaptor will need its own transaction.
		adaptor.commit();
		InstancesDeleter deleter = new InstancesDeleter();
		deleter.deleteObsoleteInstances(adaptor, pathToUnreviewedUniprotIDsFile);

		if (testMode)
		{
			logger.info("Test mode is set - Rolling back transaction...");
			//adaptor.rollback();
			adaptor.executeQuery("ROLLBACK TO " + SAVEPOINT_NAME , null);
		}
		else
		{
			logger.info("Test mode is NOT set - Committing transaction...");
			adaptor.commit();
		}
		logger.info("Done.");
	}

	private Map<String, GKInstance> getReferenceDNASequences(MySQLAdaptor adaptor)
			throws Exception, InvalidAttributeException {
		return getIdentifierMappedCollectionOfType(adaptor, ReactomeJavaConstants.ReferenceDNASequence, null);
	}

	private Map<String, GKInstance> getReferenceGeneProducts(MySQLAdaptor adaptor)
			throws Exception, InvalidAttributeException {
		return getIdentifierMappedCollectionOfType(adaptor, ReactomeJavaConstants.ReferenceGeneProduct, "UniProt");
	}

	private Map<String, GKInstance> getReferenceIsoforms(MySQLAdaptor adaptor)
			throws Exception, InvalidAttributeException {
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
		for (String k : adaptorPool.keySet())
		{
			adaptorPool.get(k).cleanUp();
		}

		logger.info("{}s {} mapped by Identifier.", reactomeClassName, instanceMap.size());

		return instanceMap;
	}

	private boolean debugXML(Properties props) {
		return Boolean.valueOf(props.getProperty("debugXML", "false"));
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
}
