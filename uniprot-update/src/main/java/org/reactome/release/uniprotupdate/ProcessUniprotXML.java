package org.reactome.release.uniprotupdate;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBResult;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

/**
 * This class contains code that can be used to process UniProt XML files.
 * @author sshorser
 *
 */
class ProcessUniprotXML {
	private static final Logger logger = LogManager.getLogger();
	private static final String ENTRY_ELEMENT_NAME = "entry";
	// Starting size of ArrayList was determined by counting the number of "<entry>" lines in the input file.
	// In fact, there were only about 530000 when I checked, but I figure it can't hurt to have a little extra room.
	private static final int EXPECTED_ENTRY_MAX_SIZE = 600000;
	private static final String PATH_TO_XSL = "src/main/resources/uniprotXMLProcessor.xsl";

	private static OutputStream debugOutputStream;

	/**
	 * This method will process a UniProt XML file, and return a list of all UniprotData objects that were extracted.
	 *
	 * @param pathToUniProtXMLFile - The path to the Uniprot XML file to process.
	 * @param debugXML True to be passed if the results of the UniProt XML transformation should be logged to a debug
	 * file; false otherwise
	 * @return An ArrayList containing UniprotData objects.
	 * @throws JAXBException Thrown if an error is encountered while creating the
	 * JAXBContext or JAXBResult
	 * @throws XMLStreamException Thrown if unable to create an XMLStreamReader or process tags for the UniProt XML file
	 * @throws FileNotFoundException Thrown if unable to find the UniProt XML file or the XSL file to transform the
	 * UniProt XML tags in the UniProt XML file
	 */
	public static List<UniprotData> getDataFromUniprotFile(String pathToUniProtXMLFile, boolean debugXML)
		throws JAXBException, XMLStreamException, FileNotFoundException {
		List<UniprotData> uniprotData = new ArrayList<>(EXPECTED_ENTRY_MAX_SIZE);

		long startTime = System.currentTimeMillis();
		XMLStreamReader xmlStreamReader = getXMLStreamReader(pathToUniProtXMLFile);
		while (xmlStreamReader.nextTag() == XMLStreamConstants.START_ELEMENT) {
			// Check to see if we found "<entry>"
			if (foundUniProtEntryTag(xmlStreamReader)) {
				JAXBResult transformedUniProtXMLResult = transformUniProtEntry(xmlStreamReader);

				if (transformedUniProtXMLResult == null) {
					// Hmmm... should I break the loop if something bad happens here?
					// Or just print the stacktrace/some custom message, and then
					// keep going? Hasn't happened yet with samples of REAL data.
					continue;
				}

				// Add the result to the list.
				uniprotData.add((UniprotData) transformedUniProtXMLResult.getResult());

				// Try to also send to a file.
				if (debugXML) {
					sendResultsToDebugFile(transformedUniProtXMLResult);
				}

				// Log results for every 10000 UniProt entries to let the users know that progress *IS* being made.
				if (uniprotData.size() % 10000 == 0) {
					logProgressSince(startTime, uniprotData.size());
				}
			}
		}
		logProgressSince(startTime, uniprotData.size());
		xmlStreamReader.close();
		return uniprotData;
	}

	private static XMLStreamReader getXMLStreamReader(String pathToUniProtXMLFile)
		throws FileNotFoundException, XMLStreamException {
		// Stream the XML file. StAX is faster than DOM (waaaaaaaaaay faster) or SAX.
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(new FileReader(pathToUniProtXMLFile));
		xmlStreamReader.nextTag(); // Advance to statements element

		return xmlStreamReader;
	}

	private static boolean foundUniProtEntryTag(XMLStreamReader xmlStreamReader) {
		return xmlStreamReader.getName().getLocalPart().equals(ENTRY_ELEMENT_NAME);
	}

	private static JAXBResult transformUniProtEntry(XMLStreamReader xmlStreamReader)
		throws JAXBException, FileNotFoundException {
		// We're also going to use XSL for parts of the document!
		// Load the XSL.
		Source xsl = new StreamSource(new FileInputStream(PATH_TO_XSL));
		// Create a new StAX source based on the current stream reader, which is pointing at "<entry>",
		StAXSource sourceUniProtData = new StAXSource(xmlStreamReader);
		// Then unmarshall that into a Java class (UniprotData).
		JAXBResult targetUniProtDataResult = new JAXBResult(JAXBContext.newInstance(UniprotData.class));

		// ...AND we'll use JAXB to take the transformed output and turn it into an object!
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer(xsl);
			transformer.transform(sourceUniProtData, targetUniProtDataResult);
			return targetUniProtDataResult;
		} catch (TransformerException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static void sendResultsToDebugFile(JAXBResult result) {
		try {
			JAXBContext marshallerContext = JAXBContext.newInstance(UniprotData.class);
			Marshaller marshaller = marshallerContext.createMarshaller();
			marshaller.setProperty("jaxb.formatted.output", true);
			marshaller.setProperty("jaxb.fragment", true);
			marshaller.marshal(result.getResult(), getDebugOutputStream());
		} catch (JAXBException | FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static OutputStream getDebugOutputStream() throws FileNotFoundException {
		if (debugOutputStream == null) {
			debugOutputStream = new FileOutputStream("simplified_uniprot_sprot.xml");
		}

		return debugOutputStream;
	}

	private static void logProgressSince(long startTime, int dataListSize) {
		long duration = System.currentTimeMillis() - startTime;

		logger.info("{} records extracted in {} seconds.",
			dataListSize,
			Duration.ofMillis(duration).toString()
		);
	}
}
