package org.reactome.release;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ModelsTSVParser {
    private static final Logger logger = LogManager.getLogger();

    private ModelsTSVParser() { }

    /**
     * Parses the contents of the models2pathways.tsv file, returning a Map of ReactomePathwayIds=[BioModelsIdentifiers,...]
     * @param tsvFile -- String, path/to/models2pathways.tsv
     * @return Map of ReactomePathwayIds and a List of BioModelsIdentifiers
     */
    public static Map<String, Set<String>> parse(String tsvFile) {
        Map<String, Set<String>> pathwayToBiomodelsIds = new HashMap<>();

        if (tsvFile == null || tsvFile.isEmpty()) {
            return pathwayToBiomodelsIds;
        }

        try(BufferedReader br = new BufferedReader(new FileReader(tsvFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t");
                String biomodelsId = fields[0];
                String pathwayStableId = fields[1];

                if (!matchBioModelsPattern(biomodelsId)) {
                    logger.warn("Line has improperly formatted BioModel ID -- skipping");
                    continue;
                }
                if (!matchStableIdPattern(pathwayStableId)) {
                    logger.warn("Line has improperly formatted or Stable ID -- skipping");
                    continue;
                }

                if (pathwayToBiomodelsIds.get(pathwayStableId) == null) {
                    Set<String> biomodelsIds = new LinkedHashSet<>();
                    biomodelsIds.add(biomodelsId);
                    pathwayToBiomodelsIds.put(pathwayStableId, biomodelsIds);
                } else {
                    pathwayToBiomodelsIds.get(pathwayStableId).add(biomodelsId);
                }
            }
        } catch (IOException e) {
            logger.error("Problem encountered processing tsvFile " + tsvFile, e);
        }

        return pathwayToBiomodelsIds;
    }

    private static boolean matchBioModelsPattern(String bioModelsId) {
        return bioModelsId != null && bioModelsId.startsWith("BIOMD");
    }

    private static boolean matchStableIdPattern(String pathwayStableId) {
        return pathwayStableId != null && pathwayStableId.matches("R-\\w{3}-\\d+");
    }
}
