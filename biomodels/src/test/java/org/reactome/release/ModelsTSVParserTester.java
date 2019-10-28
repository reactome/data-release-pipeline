package org.reactome.release;

import org.junit.Test;

import java.nio.file.Paths;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ModelsTSVParserTester {

    private final int correctPathwayCount = 3;
    private final int correctNumberPathwaysInTestFile = 745;
    private final long correctNumberBioModelsIdsInTestFile = 153L;
    private final String RESOURCE_DIR = Paths.get("src", "test", "resources").toFile().getAbsolutePath();

    @Test
    public void emptyFilenameReturnsEmptyMap() {
        Map<String, Set<String>> pathwayToBioModelsIds =  ModelsTSVParser.parse("");
        assertThat("Empty file produces empty pathwayToBioModelsIds map", pathwayToBioModelsIds.isEmpty(), is(true));
    }

    @Test
    public void unknownFileNameReturnsEmptyMap() {
        String filepath = Paths.get(RESOURCE_DIR, "filedoesnotexist.tsv").toFile().getAbsolutePath();
        Map<String, Set<String>> pathwayToBioModelsIds =  ModelsTSVParser.parse("filedoesnotexist.tsv");
        assertThat("Unknown file produces empty pathwayToBioModelsIds map", pathwayToBioModelsIds.isEmpty(), is(true));
    }

    @Test
    public void improperlyFormattedFileReturnsEmptyMap() {
        String filepath = Paths.get(RESOURCE_DIR, "improper_models2pathways.tsv").toFile().getAbsolutePath();
        Map<String, Set<String>> pathwayToBioModelsIds = ModelsTSVParser.parse(filepath);
        assertThat("Improperly formatted file produces empty pathwayToBioModelsIds map", pathwayToBioModelsIds.isEmpty(), is(true));
    }

    @Test
    public void partiallyCorrectFileReturnsPopulatedMap() {
        String filepath = Paths.get(RESOURCE_DIR, "partiallycorrect_models2pathways.tsv").toFile().getAbsolutePath();
        Map<String, Set<String>> pathwayToBioModelsIds = ModelsTSVParser.parse(filepath);
        assertThat(pathwayToBioModelsIds.keySet().size(), is(equalTo(correctPathwayCount)));
        assertThat(pathwayToBioModelsIds.get("R-MMU-1169091").contains("BIOMD0000000226"), is(equalTo(true)));
    }

    @Test
    public void testFileReturnsPopulatedMap() {
        String filepath = Paths.get(RESOURCE_DIR, "models2pathways.tsv").toFile().getAbsolutePath();
        Map<String, Set<String>> pathwayToBioModelsIds = ModelsTSVParser.parse(filepath);
        assertThat(pathwayToBioModelsIds.keySet().size(), is(equalTo(correctNumberPathwaysInTestFile)));
    }

    @Test
    public void testFileReturnsCorrectNumberOfIds() {
        String filepath = Paths.get(RESOURCE_DIR, "models2pathways.tsv").toFile().getAbsolutePath();
        Map<String, Set<String>> pathwayToBioModelsIds = ModelsTSVParser.parse(filepath);
        long bioModelsIdsCount =
                pathwayToBioModelsIds.keySet().stream().map(
                        pathway -> pathwayToBioModelsIds.get(pathway)
                ).flatMap(
                        Collection::stream
                ).distinct().count();
        assertThat(bioModelsIdsCount, is(equalTo(correctNumberBioModelsIdsInTestFile))); // Number of distinct BioModels Ids in test input file
    }

    @Test
    public void testFileContainsCorrectMapping() {
        String filepath = Paths.get(RESOURCE_DIR, "models2pathways.tsv").toFile().getAbsolutePath();
        Map<String, Set<String>> pathwayToBioModelsIds = ModelsTSVParser.parse(filepath);
        // Tests pathway contains correct BioModels id
        assertThat(pathwayToBioModelsIds.get("R-HSA-199418").contains("BIOMD0000000175"), is(equalTo(true)));
    }

    @Test
    public void testFileProducesMappingWithoutNullKeysOrValues() {
        String filepath = Paths.get(RESOURCE_DIR, "models2pathways.tsv").toFile().getAbsolutePath();
        Map<String, Set<String>> pathwayToBioModelsIds = ModelsTSVParser.parse(filepath);
        // Tests for null in Map keys and value
        assertThat("pathwayToBioModelsIds map does not contain any null keys", pathwayToBioModelsIds.containsKey(null), is(equalTo(false)));
        assertThat("pathwayToBioModelsIds map does not contain any null values", pathwayToBioModelsIds.containsValue(null), is(equalTo(false)));
    }

    @Test
    public void testFileProducesMappingWithoutListContainingNullValues() {
        String filepath = Paths.get(RESOURCE_DIR, "models2pathways.tsv").toFile().getAbsolutePath();
        Map<String, Set<String>> pathwayToBioModelsIds = ModelsTSVParser.parse(filepath);
        // Tests for null in any List object in pathwayToBioModelsIds Map
        boolean nullValueInList = false;
        for (String pathwayToBioModelsIdsKey : pathwayToBioModelsIds.keySet()) {
            if (pathwayToBioModelsIds.get(pathwayToBioModelsIdsKey).contains(null)) {
                nullValueInList = true;
            }
        }
        assertThat("No null values in any lists in pathwaysToBioModelsIds map", nullValueInList, is(equalTo(false)));
    }
}
