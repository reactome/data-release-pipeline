package org.reactome.release.uniprotupdate;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.reactome.release.common.ReleaseStep;
import org.reactome.release.common.database.InstanceEditUtils;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

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
		boolean debugXML = Boolean.valueOf( props.getProperty("debugXML", "false") );
				
		List<UniprotData> uniprotData = ProcessUniprotXML.getDataFromUniprotFile(pathToUniprotFile, debugXML);
		UniprotUpdater updater = new UniprotUpdater();
		String creatorName = this.getClass().getName();
		GKInstance instanceEdit = InstanceEditUtils.createInstanceEdit(adaptor, Long.valueOf(personID), creatorName );
		
		Map<String, GKInstance> referenceIsoforms = getIdentifierMappedCollectionOfType(adaptor, ReactomeJavaConstants.ReferenceIsoform, "UniProt");
		logger.info("{} ReferenceIsoforms mapped by Identifier.", referenceIsoforms.size());
		Map<String, GKInstance> referenceGeneProducts = getIdentifierMappedCollectionOfType(adaptor, ReactomeJavaConstants.ReferenceGeneProduct, "UniProt");
		logger.info("{} ReferenceGeneProducts mapped by Identifier.", referenceGeneProducts.size());
		Map<String, GKInstance> referenceDNASequences = getIdentifierMappedCollectionOfType(adaptor, ReactomeJavaConstants.ReferenceDNASequence, null);
		logger.info("{} ReferenceDNASequences mapped by Identifier.", referenceDNASequences.size());
		adaptor.executeQuery("SAVEPOINT " + SAVEPOINT_NAME , null);
		adaptor.startTransaction();
		updater.updateUniprotInstances(adaptor, uniprotData, referenceDNASequences, referenceGeneProducts, referenceIsoforms, instanceEdit);
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

	/**
	 * Gets a map of instances, keyed by identifier.
	 * @param adaptor - the database adaptor to use.
	 * @param reactomeClassName - the Reactome "type" to which the instances will be constrained.
	 * @param refDBName - the name of the reference database to which the instances will be constrained.
	 * @return
	 * @throws Exception
	 * @throws InvalidAttributeException
	 */
	private Map<String, GKInstance> getIdentifierMappedCollectionOfType(MySQLAdaptor adaptor, String reactomeClassName, String refDBName) throws Exception, InvalidAttributeException
	{
		@SuppressWarnings("unchecked")
		Collection<GKInstance> instances = (Collection<GKInstance>)adaptor.fetchInstancesByClass(reactomeClassName);
		Map<String, GKInstance> instanceMap = Collections.synchronizedMap(new HashMap<String, GKInstance>(instances.size()));
		//for (GKInstance instance : instances)
		Map<String, MySQLAdaptor> adaptorPool = Collections.synchronizedMap(new HashMap<String, MySQLAdaptor>());
		
		instances.parallelStream().forEach( instance -> 
		{
			try
			{
				MySQLAdaptor tmpAdaptor;
				if (adaptorPool.containsKey(Thread.currentThread().getName()))
				{
					tmpAdaptor = adaptorPool.get(Thread.currentThread().getName());
					instance.setDbAdaptor(tmpAdaptor);
				}
				else
				{
					tmpAdaptor = new MySQLAdaptor( ((MySQLAdaptor)instance.getDbAdaptor()).getDBHost(), ((MySQLAdaptor)instance.getDbAdaptor()).getDBName(), ((MySQLAdaptor)instance.getDbAdaptor()).getDBUser(), ((MySQLAdaptor)instance.getDbAdaptor()).getDBPwd() ,((MySQLAdaptor)instance.getDbAdaptor()).getDBPort());
					adaptorPool.put(Thread.currentThread().getName(), tmpAdaptor);
					instance.setDbAdaptor(tmpAdaptor);
				}
				String identifier;
				try
				{
					identifier = (String) instance.getAttributeValue(ReactomeJavaConstants.identifier);
					if (identifier !=null && identifier.length() > 0)
					{
						// fast-load the attributes now so accessing them later will be faster.
						// tmpAdaptor.fastLoadInstanceAttributeValues(instance);
						// Specify a ReferenceDatabase name that the instances should be constrained by.
						if (refDBName!=null)
						{
							GKInstance refDB = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
							if (refDB != null && refDBName.equals(refDB.getAttributeValue(ReactomeJavaConstants.name)))
							{
								instanceMap.put(identifier, instance);
							}
						}
						else
						{
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
		
		return instanceMap;
	}

}
