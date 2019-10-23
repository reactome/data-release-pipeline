package org.reactome.release;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ModelsTSVParserTester {

    private final int correctPathwayCount = 3;
    private final int correctNumberPathwaysInTestFile = 745;
    private final int correctNumberBioModelsIdsInTestFile = 153;
    @Test
    public void emptyMapFromEmptyFileName() {
        Map<String, List<String>> pathwayToBioModelsIds =  ModelsTSVParser.parse("");
        assertTrue("Empty file produces empty pathwayToBioModelsIds map", pathwayToBioModelsIds.isEmpty());
    }

    @Test
    public void emptyMapFromUnknownFileName() {
        Map<String, List<String>> pathwayToBioModelsIds =  ModelsTSVParser.parse("filedoesnotexist.tsv");
        assertTrue("Unknown file produces empty pathwayToBioModelsIds map", pathwayToBioModelsIds.isEmpty());
    }

    @Test
    public void emptyMapFromImproperFile() {
        Map<String, List<String>> pathwayToBioModelsIds = ModelsTSVParser.parse("src/test/resources/improper_models2pathways.tsv");
        assertTrue("Improperly formatted file produces empty pathwayToBioModelsIds map", pathwayToBioModelsIds.isEmpty());
    }

    @Test
    public void populatedMapFromPartiallyCorrectFile() {
        Map<String, List<String>> pathwayToBioModelsIds = ModelsTSVParser.parse("src/test/resources/partiallycorrect_models2pathways.tsv");
        assertEquals(pathwayToBioModelsIds.keySet().size(), correctPathwayCount);
        assertTrue(pathwayToBioModelsIds.get("R-MMU-1169091").contains("BIOMD0000000226"));
    }

    @Test
    public void testModels2PathwaysCorrectlyParsed() {
        Map<String, List<String>> pathwayToBioModelsIds =
            ModelsTSVParser.parse("src/test/resources/models2pathways.tsv");
        assertEquals(pathwayToBioModelsIds.keySet().size(), correctNumberPathwaysInTestFile);

        long bioModelsIdsCount =
            pathwayToBioModelsIds.keySet().stream().map(
                pathway -> pathwayToBioModelsIds.get(pathway)
            ).flatMap(
                Collection::stream
            ).distinct().count();
        assertEquals(bioModelsIdsCount, correctNumberBioModelsIdsInTestFile); // Number of distinct BioModels Ids in test input file

        // Tests pathway contains correct BioModels id
        assertTrue(pathwayToBioModelsIds.get("R-HSA-199418").contains("BIOMD0000000175"));

        // Tests for null in Map keys and value
        assertTrue("pathwayToBioModelsIds map does not contain any null keys", !pathwayToBioModelsIds.containsKey(null));
        assertTrue("pathwayToBioModelsIds map does not contain any null values", !pathwayToBioModelsIds.containsValue(null));

        // Tests for null in any List object in pathwayToBioModelsIds Map
        boolean nullValueInList = false;
        for (String pathwayToBioModelsIdsKey : pathwayToBioModelsIds.keySet()) {
            if (pathwayToBioModelsIds.get(pathwayToBioModelsIdsKey).contains(null)) {
                nullValueInList = true;
            }
        }
        assertTrue("No null values in any lists in pathwaysToBioModelsIds map", !nullValueInList);
    }
}
