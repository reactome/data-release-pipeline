package org.reactome.release.uniprotupdate;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.reactome.release.uniprotupdate.dataschema.UniprotData;


public class ProcessUniprotXML
{

	private static final String ENTRY_ELEMENT_NAME = "entry";
	private static final String PATH_TO_XSL = "src/main/resources/uniprotXMLProcessor.xsl";

	public static List<UniprotData> getDataFromUniprotFile(String pathToFile) throws JAXBException, XMLStreamException, TransformerConfigurationException, FileNotFoundException
	{
		List<UniprotData> uniprotData = new ArrayList<UniprotData>(600000);
		
		XMLInputFactory xif = XMLInputFactory.newInstance();
		XMLStreamReader xsr = xif.createXMLStreamReader(new FileReader(pathToFile));
		xsr.nextTag(); // Advance to statements element

		TransformerFactory tf = TransformerFactory.newInstance();
		Source xsl = new StreamSource(new FileInputStream(PATH_TO_XSL));
		Transformer t = tf.newTransformer(xsl );
		long startTime = System.currentTimeMillis();
		JAXBContext unmarshaller = JAXBContext.newInstance(org.reactome.release.uniprotupdate.dataschema.UniprotData.class);
		while (xsr.nextTag() == XMLStreamConstants.START_ELEMENT)
		{
			if (xsr.getName().getLocalPart().equals(ENTRY_ELEMENT_NAME))
			{
				try
				{
					JAXBResult result = new JAXBResult(unmarshaller);
					t.transform(new StAXSource(xsr), result);
					uniprotData.add((UniprotData)result.getResult());
				}
				catch (TransformerException e)
				{
					e.printStackTrace();
				}
				catch (JAXBException e)
				{
					e.printStackTrace();
				}
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
	
//	public static void main(String[] args) throws FileNotFoundException, XMLStreamException, TransformerException, JAXBException
//	{
//		XMLInputFactory xif = XMLInputFactory.newInstance();
//		XMLStreamReader xsr = xif.createXMLStreamReader(new FileReader("src/main/resources/uniprot_sprot.xml"));
//		xsr.nextTag(); // Advance to statements element
//
//		TransformerFactory tf = TransformerFactory.newInstance();
//		Source xsl = new StreamSource(new FileInputStream(PATH_TO_XSL));
//		OutputStream output = new FileOutputStream("simplified_uniprot.out");
//		List<UniprotData> uniprotData = new ArrayList<UniprotData>(/*600000*/);
//		Transformer t = tf.newTransformer(xsl );
//		//t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
//		long startTime = System.currentTimeMillis();
//		int recordCount = 0;
//		JAXBContext unmarshaller = JAXBContext.newInstance(org.reactome.release.uniprotupdate.dataschema.UniprotData.class);
//		while (xsr.nextTag() == XMLStreamConstants.START_ELEMENT)
//		{
//			if (xsr.getName().getLocalPart().equals("entry"))
//			{
//				recordCount++;
//				//
//				//DOMResult result = new DOMResult();
////				StreamResult result = new StreamResult(output);
//				//StreamResult result = new StreamResult(System.out);
//				JAXBResult result = new JAXBResult(unmarshaller);
//				t.transform(new StAXSource(xsr), result);
//				uniprotData.add((UniprotData)result.getResult());
////				System.out.println( ((UniprotData)result.getResult()).getRecommendedName() + "; "
////									+ ((UniprotData)result.getResult()).getScientificName() + "; "
////									+ ((UniprotData)result.getResult()).getSequenceChecksum() + "; "
////									+ ((UniprotData)result.getResult()).getSequenceLength() + "; "
////									+ ((UniprotData)result.getResult()).getAccessions() + "; "
////									+ ((UniprotData)result.getResult()).getChains() + "; "
////									+ ((UniprotData)result.getResult()).getGeneNames() + "; "
////									+ ((UniprotData)result.getResult()).getIsoforms() + "; "
////									+ ((UniprotData)result.getResult()).getKeywords() + ".");
//				//Node domNode = result.getNode();
//				if (recordCount%10000 == 0)
//				{
//					long endTime = System.currentTimeMillis();
//					
//					System.out.println(recordCount + " records processed in " + Duration.ofMillis(endTime - startTime).toString() + " seconds.");
//				}
//			}
//		}
//		long endTime = System.currentTimeMillis();
//		System.out.println(recordCount + " records processed in " + Duration.ofMillis(endTime - startTime).toString() + " seconds.");
//	}

}
