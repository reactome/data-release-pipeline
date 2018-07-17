package org.reactome.release.uniprotupdate;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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

	@Override
	public void executeStep(Properties props) throws Exception
	{
		String pathToFile = props.getProperty("pathToFile");
		MySQLAdaptor adaptor = getMySQLAdaptorFromProperties(props);
		String personID = props.getProperty("personID"); 
		List<UniprotData> uniprotData = ProcessUniprotXML.getDataFromUniprotFile(pathToFile);
		UniprotUpdater updater = new UniprotUpdater();
		String creatorName = this.getClass().getName();
		GKInstance instanceEdit = InstanceEditUtils.createInstanceEdit(adaptor, Long.valueOf(personID), creatorName );
		
		Map<String, GKInstance> referenceIsoforms = getIdentifierMappedCollectionOfType(adaptor, ReactomeJavaConstants.ReferenceIsoform);
		System.out.println(referenceIsoforms.size() + " ReferenceIsoforms mapped by Identifier.");
		Map<String, GKInstance> referenceGeneProducts = getIdentifierMappedCollectionOfType(adaptor, ReactomeJavaConstants.ReferenceGeneProduct);
		System.out.println(referenceGeneProducts.size() + " ReferenceGeneProducts mapped by Identifier.");
		Map<String, GKInstance> referenceDNASequences = getIdentifierMappedCollectionOfType(adaptor, ReactomeJavaConstants.ReferenceDNASequence);
		System.out.println(referenceDNASequences.size() + " ReferenceDNASequences mapped by Identifier.");
		updater.updateUniprotInstances(adaptor, uniprotData, referenceDNASequences, referenceGeneProducts, referenceIsoforms, instanceEdit);
		
	}

	private Map<String, GKInstance> getIdentifierMappedCollectionOfType(MySQLAdaptor adaptor, String reactomeClassName) throws Exception, InvalidAttributeException
	{
		@SuppressWarnings("unchecked")
		Collection<GKInstance> instances = (Collection<GKInstance>)adaptor.fetchInstancesByClass(reactomeClassName);
		Map<String, GKInstance> instanceMap = new HashMap<String, GKInstance>(instances.size());
		for (GKInstance instance : instances)
		{
			String identifier = (String) instance.getAttributeValue(ReactomeJavaConstants.identifier);
			if (identifier !=null && identifier.length() > 0)
			{
				instanceMap.put(identifier, instance);
			}
		}
		return instanceMap;
	}

}
