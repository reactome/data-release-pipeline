package org.reactome.release;

import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ModelsTSVParserTester {
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
        assertEquals(pathwayToBioModelsIds.keySet().size(), 3);
        assertTrue(pathwayToBioModelsIds.get("R-MMU-1169091").contains("BIOMD0000000226"));
    }

    @Test
    public void testModels2PathwaysCorrectlyParsed() {
        Map<String, List<String>> pathwayToBioModelsIds =
            ModelsTSVParser.parse("src/test/resources/models2pathways.tsv");
        assertEquals(pathwayToBioModelsIds.keySet().size(), 745); // Number of pathways in test input file

        long bioModelsIdsCount =
            pathwayToBioModelsIds.keySet().stream().map(
                pathway -> pathwayToBioModelsIds.get(pathway)
            ).flatMap(
                Collection::stream
            ).distinct().count();
        assertEquals(bioModelsIdsCount, 153); // Number of distinct BioModels Ids in test input file

        // Tests pathway contains correct BioModels id
        assertTrue(pathwayToBioModelsIds.get("R-HSA-199418").contains("BIOMD0000000175"));
    }
}
