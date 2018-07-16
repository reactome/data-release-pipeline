package org.reactome.release.uniprotupdate;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBResult;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import org.reactome.release.uniprotupdate.dataschema.UniprotData;

/**
 * This class contains code that can be used to process Uniprot XML files.
 * @author sshorser
 *
 */
public class ProcessUniprotXML
{

	private static final String ENTRY_ELEMENT_NAME = "entry";
	private static final String PATH_TO_XSL = "src/main/resources/uniprotXMLProcessor.xsl";

	/**
	 * This function will process a Uniprot XML file, and return a list of all UniprotData objects that were extracted.
	 * @param pathToFile - The path to the Uniprot file to process.
	 * @return An ArrayList containing UniprotData objects.
	 * @throws JAXBException
	 * @throws XMLStreamException
	 * @throws TransformerConfigurationException
	 * @throws FileNotFoundException
	 */
	public static List<UniprotData> getDataFromUniprotFile(String pathToFile) throws JAXBException, XMLStreamException, TransformerConfigurationException, FileNotFoundException
	{
		// Starting size of ArrayList was determined by counting the number of "<entry>" lines in the input file.
		// In fact, there were only about 530000 when I checked, but I figure it can't hurt to have a little extra room.
		List<UniprotData> uniprotData = new ArrayList<UniprotData>(600000);
		
		// Stream the XML file. StAX is faster than DOM (waaaaaaaaaay faster) or SAX.
		XMLInputFactory xif = XMLInputFactory.newInstance();
		XMLStreamReader xsr = xif.createXMLStreamReader(new FileReader(pathToFile));
		xsr.nextTag(); // Advance to statements element

		// But... we're also going to use XSL for parts of the document!
		TransformerFactory tf = TransformerFactory.newInstance();
		// Load the XSL.
		Source xsl = new StreamSource(new FileInputStream(PATH_TO_XSL));
		Transformer transformer = tf.newTransformer(xsl);
		// ...AND we'll use JAXB to take the transformed output and turn it into an object!
		JAXBContext unmarshaller = JAXBContext.newInstance(org.reactome.release.uniprotupdate.dataschema.UniprotData.class);
		long startTime = System.currentTimeMillis();
		while (xsr.nextTag() == XMLStreamConstants.START_ELEMENT)
		{
			// Check to see if we found "<entry>"
			if (xsr.getName().getLocalPart().equals(ENTRY_ELEMENT_NAME))
			{
				try
				{
					// Create a new StAX source based on the current stream reader, which is pointing at "<entry>",
					// and then pass that to the transformer, and then unmarshall that into a Java class (UniprotData).
					JAXBResult result = new JAXBResult(unmarshaller);
					transformer.transform(new StAXSource(xsr), result);
					// Add the result to the list.
					uniprotData.add((UniprotData)result.getResult());
				}
				// Hmmm... should I break the loop if something bad happens here? Or just print the stacktrace/some custom message, and then
				// keep going? Hasn't happened yet with samples of REAL data.
				catch (TransformerException e)
				{
					e.printStackTrace();
				}
				catch (JAXBException e)
				{
					e.printStackTrace();
				}
				// Just to let the users know that progress *IS* being made.
				if (uniprotData.size()%10000 == 0)
				{
					long endTime = System.currentTimeMillis();
					
					System.out.println(uniprotData.size() + " records processed in " + Duration.ofMillis(endTime - startTime).toString() + " seconds.");
				}
			}
		}
		long endTime = System.currentTimeMillis();
		System.out.println(uniprotData.size() + " records processed in " + Duration.ofMillis(endTime - startTime).toString() + " seconds.");
		
		return uniprotData;
	}
}
