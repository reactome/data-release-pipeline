package org.reactome.release.dataexport;

import org.junit.jupiter.api.*;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.reactome.release.dataexport.DbIdGenerator.getNextDBID;

public class NCBIEntryTest {
	private NCBIEntry entry1;
	private NCBIEntry entry2;

	@BeforeEach
	public void createTestFixtures() {
		final String FIRST_UNIPROT_ACCESSION = "Q12345";
		final Set<String> FIRST_DUMMY_SET_OF_NCBI_IDS = new HashSet<>(Arrays.asList("1", "2"));

		final String SECOND_UNIPROT_ACCESSION = "P23456";
		final Set<String> SECOND_DUMMY_SET_OF_NCBI_IDS = new HashSet<>(Arrays.asList("3", "4"));

		entry1 = NCBIEntryTestFixture.createNCBIEntry(FIRST_UNIPROT_ACCESSION, FIRST_DUMMY_SET_OF_NCBI_IDS);
		entry2 = NCBIEntryTestFixture.createNCBIEntry(SECOND_UNIPROT_ACCESSION, SECOND_DUMMY_SET_OF_NCBI_IDS);
	}

	@Test
	public void noNCBIEntriesForEmptyDatabase() {
		DummyGraphDBServer dummyGraphDBServer = DummyGraphDBServer.getInstance();
		dummyGraphDBServer.initializeNeo4j();

		List<NCBIEntry> ncbiEntries = NCBIEntry.getUniProtToNCBIGeneEntries(dummyGraphDBServer.getSession());

		assertThat(ncbiEntries, is(empty()));
	}


	@Test
	public void retrievesNCBIEntryFromDummyGraphDB() {
		final long UNIPROT_DB_ID = 69487L;
		final String UNIPROT_ACCESSION = "P04637";
		final String UNIPROT_DISPLAY_NAME = "UniProt:P04637 TP53";
		final String NCBI_GENE_ID = "5339";

		NCBIEntry expectedNCBIEntry = new NCBIEntry(
			UNIPROT_DB_ID,
			UNIPROT_ACCESSION,
			UNIPROT_DISPLAY_NAME,
			new HashSet<>(Collections.singletonList(NCBI_GENE_ID))
		);

		DummyGraphDBServer dummyGraphDBServer = DummyGraphDBServer.getInstance();
		dummyGraphDBServer.initializeNeo4j();
		dummyGraphDBServer.populateDummyGraphDB();

		List<NCBIEntry> ncbiEntries = NCBIEntry.getUniProtToNCBIGeneEntries(dummyGraphDBServer.getSession());

		assertThat(ncbiEntries, hasItem(expectedNCBIEntry));
	}

	@Test
	public void sortsByUniProtAccessionAscendingly() {
		List<NCBIEntry> ncbiEntries = new ArrayList<>();
		ncbiEntries.add(entry1);
		ncbiEntries.add(entry2);

		Collections.sort(ncbiEntries);

		assertThat(ncbiEntries, contains(entry2, entry1));
	}

	@Test
	public void getEventLinkXML() {
		final String NCBI_GENE_ID = "12345";
		final List<String> EXPECTED_STRINGS = Arrays.asList(
			"<ObjId>" + NCBI_GENE_ID + "</ObjId>",
			"<Base>&event.base.url;</Base>",
			"<Rule>" + NCBIEntryTestFixture.EVENT_STABLE_ID + "</Rule>",
			"<UrlName>Reactome Event:" + NCBIEntryTestFixture.EVENT_NAME
		);

		String eventLinkXML = entry1.getEventLinkXML(NCBI_GENE_ID, NCBIEntryTestFixture.EVENT);

		for (String expectedString : EXPECTED_STRINGS) {
			assertThat(eventLinkXML, containsString(expectedString));
		}
	}

	@Test
	public void getEntityLinkXML() {
		final String NCBI_GENE_ID = "12345";
		final List<String> EXPECTED_STRINGS = Arrays.asList(
			"<ObjId>" + NCBI_GENE_ID + "</ObjId>",
			"<Base>&entity.base.url;</Base>",
			"<Rule>" + entry1.getUniprotAccession() + "</Rule>",
			"<UrlName>Reactome Entity:" + entry1.getUniprotAccession()
		);

		String entityLinkXML = entry1.getEntityLinkXML(NCBI_GENE_ID);

		for (String expectedString : EXPECTED_STRINGS) {
			assertThat(entityLinkXML, containsString(expectedString));
		}
	}

	private static class NCBIEntryTestFixture {
		private NCBIEntry entry;

		private static final String EVENT_NAME = "test event";
		private static final String EVENT_STABLE_ID = "R-HSA-54321";
		private static final ReactomeEvent EVENT = new ReactomeEvent(getNextDBID(), EVENT_NAME, EVENT_STABLE_ID);

		private NCBIEntryTestFixture(String uniProtAccession, Set<String> ncbiIds) {
			entry = new NCBIEntry(
				getNextDBID(),
				uniProtAccession,
				createDummyDisplayName(uniProtAccession),
				ncbiIds
			);
		}

		public static NCBIEntry createNCBIEntry(String uniProtAccession, Set<String> ncbiIds) {
			return new NCBIEntryTestFixture(uniProtAccession, ncbiIds).getNCBIEntry();
		}

		private NCBIEntry getNCBIEntry() {
			return this.entry;
		}

		private String createDummyDisplayName(String uniProtAccession) {
			return "UniProt:" + uniProtAccession + " GENE_NAME";
		}
	}
}