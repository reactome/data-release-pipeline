package org.reactome.release.downloadDirectory;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;

public class HumanPathwaysWithDiagrams {

    private static final String humanPathwaysWithDiagramsFilename = "humanPathwaysWithDiagrams.txt";
    private static final Integer HUMAN_DATABASE_ID = 48887;

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
            if (isDiease(humanPathway)) {
                isDisease = true;
            }
            Collection<GKInstance> representedPathwayInstances = humanPathway.getReferers(ReactomeJavaConstants.representedPathway);
            // If returned value from 'representedPathway' referral is not null, it is considered to have a pathway diagram
            if (representedPathwayInstances != null) {
                // Generate line in file and append to that file
                String line = humanPathway.getDBID() + "\t" + humanPathway.getDisplayName() + "\t" + isDisease + "\n";
                Files.write(Paths.get(humanPathwaysWithDiagramsFilename), line.getBytes(), StandardOpenOption.APPEND);
            }
        }
        // Move file to directory pertaining to release
        String outpathName = releaseNumber + "/" + humanPathwaysWithDiagramsFilename;
        Files.move(Paths.get(humanPathwaysWithDiagramsFilename), Paths.get(outpathName), StandardCopyOption.REPLACE_EXISTING);
    }

    // Check if disease attribute is populated
    private static boolean isDiease(GKInstance humanPathway) throws Exception {
        return humanPathway.getAttributeValue(ReactomeJavaConstants.disease) != null;
    }
}
