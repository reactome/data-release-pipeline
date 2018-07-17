package org.reactome.release.uniprotupdate;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * 
 * @author sshorser
 *
 */
public class Main
{

	public static void main(String[] args) throws FileNotFoundException, IOException
	{
		String fileName = "uniprot-update.properties";
		
		if (args.length > 0)
		{
			fileName = args[0];
		}
		
		Properties props = new Properties();
		props.load(new FileInputStream(fileName));

		UniprotUpdateStep step = new UniprotUpdateStep();
		try
		{
			step.executeStep(props);
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
