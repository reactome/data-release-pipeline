package org.reactome.release.uniprotupdate;

import java.util.List;
import java.util.Properties;

import org.reactome.release.common.ReleaseStep;
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
		
		List<UniprotData> uniprotData = ProcessUniprotXML.getDataFromUniprotFile(pathToFile);
		
	}

}
