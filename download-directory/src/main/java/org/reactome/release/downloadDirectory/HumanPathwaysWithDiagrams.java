package org.reactome.release.downloadDirectory;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;

public class HumanPathwaysWithDiagrams {

    private static final String humanPathwaysWithDiagramsFilename = "humanPathwaysWithDiagrams.txt";
    private static final Integer HUMAN_DATABASE_ID = 48887;
    private static final String PROCESS_NODE_STRING = "org.gk.render.ProcessNode";
    private static final String RENDERABLE_COMPARTMENT_STRING = "org.gk.render.RenderableCompartment";

    /**
     * This class goes through all Human Pathway instances checking if it has a PathwayDiagram associated with it.
     * If it does, it is recorded in a file containing the Pathways db id, name and if it has a disease instance associated with it.
     * @param dba -- MySQLAdaptor
     * @param releaseNumber -- String, release number
     * @throws Exception
     */
    public static void execute(MySQLAdaptor dba, String releaseNumber) throws Exception {

        // Create new file
        File humanPathwaysWithDiagramsFile = new File(humanPathwaysWithDiagramsFilename);
        humanPathwaysWithDiagramsFile.delete();
        humanPathwaysWithDiagramsFile.createNewFile();

        // Fetch all Human Pathway instances
        Collection<GKInstance> humanPathways = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Pathway, ReactomeJavaConstants.species, "=", HUMAN_DATABASE_ID);

        for (GKInstance humanPathway : humanPathways) {
            // Default disease value is false
            boolean isDisease = false;

            // If returned value from 'disease' query is not null, its considered to be a disease-containing Pathway
            if (isDisease(humanPathway)) {
                isDisease = true;
            }

            Collection<GKInstance> representedPathwayInstances = humanPathway.getReferers(ReactomeJavaConstants.representedPathway);
            // If returned value from 'representedPathway' referral is not null, it is considered to have a pathway diagram

            if (representedPathwayInstances != null && !onlyGreenBoxDiagrams(representedPathwayInstances)) {
                // Generate line in file and append to that file
                String line = humanPathway.getDBID() + "\t" + humanPathway.getDisplayName() + "\t" + isDisease + "\n";
                Files.write(Paths.get(humanPathwaysWithDiagramsFilename), line.getBytes(), StandardOpenOption.APPEND);
            }
        }
        // Move file to directory pertaining to release
        String outpathName = releaseNumber + "/" + humanPathwaysWithDiagramsFilename;
        Files.move(Paths.get(humanPathwaysWithDiagramsFilename), Paths.get(outpathName), StandardCopyOption.REPLACE_EXISTING);
    }

    // Method for checking if diagrams are only comprised of GreenBoxDiagrams/Compartments, which we want to exclude.
    // Involves going through the PathwayDiagram XML and searching for particular strings that denote the green boxes and compartments.
    private static boolean onlyGreenBoxDiagrams(Collection<GKInstance> representedPathwayInstances) throws Exception {
        DocumentBuilderFactory documentBuildFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuildFactory.newDocumentBuilder();

        for (GKInstance pathwayDiagramInst : representedPathwayInstances) {
            String pathwayDiagramXML = pathwayDiagramInst.getAttributeValue(ReactomeJavaConstants.storedATXML).toString();
            Document parsedXMLDoc = documentBuilder.parse(new InputSource(new StringReader(pathwayDiagramXML)));
            parsedXMLDoc.getDocumentElement().normalize();
            NodeList nList = parsedXMLDoc.getElementsByTagName("Nodes");
            for (int n = 0; n < nList.getLength(); n++) {
                Node node = nList.item(n);
                NodeList nChildList = node.getChildNodes();
                for (int c = 0; c < nChildList.getLength(); c++) {
                    if (isNotAPathwayOrCompartmentNode(nChildList.item(c))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isNotAPathwayOrCompartmentNode(Node childNode) {
        return childNode.getNodeType() == Node.ELEMENT_NODE &&
            !childNode.getNodeName().equals(PROCESS_NODE_STRING) &&
            !childNode.getNodeName().equals(RENDERABLE_COMPARTMENT_STRING);
    }

    // Check if disease attribute is populated
    private static boolean isDisease(GKInstance humanPathway) throws Exception {
        return humanPathway.getAttributeValue(ReactomeJavaConstants.disease) != null;
    }
}
