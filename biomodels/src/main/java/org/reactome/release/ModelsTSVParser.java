package org.reactome.release;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelsTSVParser {
    private static final Logger logger = LogManager.getLogger();

    private ModelsTSVParser() { }

    public static Map<String, List<String>> parse(String tsvFile) {
        Map<String, List<String>> pathwayToBiomodelsIds = new HashMap<>();

        try(BufferedReader br = new BufferedReader(new FileReader(tsvFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t");
                String biomodelsId = fields[0];
                String pathwayStableId = fields[1];

                if (!matchBioModelsPattern(biomodelsId) || !matchStableIdPattern(pathwayStableId)) {
                    continue;
                }

                if (pathwayToBiomodelsIds.get(pathwayStableId) == null) {
                    List<String> biomodelsIds = new ArrayList<>();
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
        return bioModelsId != null && !bioModelsId.startsWith("BIOMD");
    }

    private static boolean matchStableIdPattern(String pathwayStableId) {
        return pathwayStableId != null || pathwayStableId.matches("R-\\w{3}-\\d+");
    }
}
