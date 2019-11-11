package org.reactome.release.dataexport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.reactome.release.dataexport.DbIdGenerator.getNextDBID;

public class UniProtReactomeEntryTest {
	private UniProtReactomeEntry canonicalUniProtReactomeEntry;
	private UniProtReactomeEntry isoformUniProtReactomeEntry;
	private final String DUMMY_UNIPROT_ACCESSION = "O65432";
	private final String DUMMY_UNIPROT_DISPLAY_NAME = "UniProt:another test UniProt";

	@BeforeEach
	public void createUniProtReactomeEntries() {
		final long CANONICAL_UNIPROT_DB_ID = 69487L;
		final String CANONICAL_UNIPROT_ACCESSION = "P04637";
		final String CANONICAL_UNIPROT_DISPLAY_NAME = "UniProt:P04637 TP53";

		final long ISOFORM_UNIPROT_DB_ID = 152926L;
		final String ISOFORM_UNIPROT_ACCESSION = "O75916-3";
		final String ISOFORM_UNIPROT_DISPLAY_NAME = "UniProt:O75916-3 RGS9";

		canonicalUniProtReactomeEntry = UniProtReactomeEntry.get(
			CANONICAL_UNIPROT_DB_ID, CANONICAL_UNIPROT_ACCESSION, CANONICAL_UNIPROT_DISPLAY_NAME
		);
		isoformUniProtReactomeEntry = UniProtReactomeEntry.get(
			ISOFORM_UNIPROT_DB_ID, ISOFORM_UNIPROT_ACCESSION, ISOFORM_UNIPROT_DISPLAY_NAME
		);
	}

	@Test
	public void sameUniProtObjectIsEqual() {
		assertThat(
			canonicalUniProtReactomeEntry,
			sameInstance(
				UniProtReactomeEntry.get(
					canonicalUniProtReactomeEntry.getDbId(),
					canonicalUniProtReactomeEntry.getAccession(),
					canonicalUniProtReactomeEntry.getDisplayName()
				)
			)
		);
	}

	@Test
	public void differentUniProtObjectsWithDifferentValuesNotEqual() {
		final String DIFFERENT_UNIPROT_ACCESSION = "Q54321";
		final String DIFFERENT_DISPLAY_NAME = "UniProt:" + DIFFERENT_UNIPROT_ACCESSION;

		assertThat(
			canonicalUniProtReactomeEntry,
			is(not(equalTo(
				UniProtReactomeEntry.get(getNextDBID(), DIFFERENT_UNIPROT_ACCESSION, DIFFERENT_DISPLAY_NAME))
			))
		);
	}

	@Test
	public void sortsByUniProtAccessionAscendingly() {
		final String UNIPROT_ACCESSION_THAT_SHOULD_BE_FIRST = "A0A234";

		List<UniProtReactomeEntry> uniProtReactomeEntries = new ArrayList<>();
		uniProtReactomeEntries.add(canonicalUniProtReactomeEntry);
		UniProtReactomeEntry uniProtReactomeEntryAddedSecond = UniProtReactomeEntry.get(
			getNextDBID(),
			UNIPROT_ACCESSION_THAT_SHOULD_BE_FIRST,
			DUMMY_UNIPROT_DISPLAY_NAME
		);
		uniProtReactomeEntries.add(uniProtReactomeEntryAddedSecond);

		Collections.sort(uniProtReactomeEntries);

		assertThat(uniProtReactomeEntries, contains(uniProtReactomeEntryAddedSecond, canonicalUniProtReactomeEntry));
	}

	@Test
	public void isoformAccessionIsAccepted() {
		final String UNIPROT_ISOFORM_ACCESSION = "P12345-1";

		UniProtReactomeEntry uniprot = UniProtReactomeEntry.get(
			getNextDBID(), UNIPROT_ISOFORM_ACCESSION, DUMMY_UNIPROT_DISPLAY_NAME
		);

		assertThat(uniprot.getAccession(), equalTo(UNIPROT_ISOFORM_ACCESSION));
	}

	@Test
	public void illegalIsoformAccessionThrowsIllegalArgumentException() {
		final String ILLEGAL_ISOFORM_ACCESSION = "P123456-1"; // 7 character parent accession is illegal in UniProt

		IllegalArgumentException thrown = assertThrows(
			IllegalArgumentException.class,
			() -> UniProtReactomeEntry.get(
				getNextDBID(), ILLEGAL_ISOFORM_ACCESSION, DUMMY_UNIPROT_DISPLAY_NAME
			),
			"Expected call to 'UniProtReactomeEntry.get' to throw due to improper UniProt accession, but it didn't"
		);

		assertThat(thrown.getMessage(), containsString("not a legal UniProt accession"));
	}

	@Test
	public void tenCharacterAccessionIsAccepted() {
		final String UNIPROT_ACCESSION = "A0A123A456";
		UniProtReactomeEntry uniprot = UniProtReactomeEntry.get(
			getNextDBID(), UNIPROT_ACCESSION, DUMMY_UNIPROT_DISPLAY_NAME
		);

		assertThat(uniprot.getAccession() , equalTo(UNIPROT_ACCESSION));
	}

	@Test
	public void illegalUniProtAccessionThrowsIllegalArgumentException() {
		final String ILLEGAL_UNIPROT_ACCESSION = "P123456"; // 7 character accession is illegal in UniProt

		IllegalArgumentException thrown = assertThrows(
			IllegalArgumentException.class,
			() -> UniProtReactomeEntry.get(
				getNextDBID(), ILLEGAL_UNIPROT_ACCESSION, DUMMY_UNIPROT_DISPLAY_NAME
			),
			"Expected call to 'UniProtReactomeEntry.get' to throw due to an illegal UniProt accession, but it didn't"
		);

		assertThat(thrown.getMessage(), containsString("not a legal UniProt accession"));
	}

	@Test
	public void incorrectDisplayNameThrowsIllegalArgumentException() {
		final String INCORRECT_DISPLAY_NAME = "testing"; // Display name must begin with "UniProt:"

		IllegalArgumentException thrown = assertThrows(
			IllegalArgumentException.class,
			() -> UniProtReactomeEntry.get(
				getNextDBID(), DUMMY_UNIPROT_ACCESSION, INCORRECT_DISPLAY_NAME
			),
			"Expected call to 'UniProtReactomeEntry.get' to throw due to an improper UniProt display name, but it " +
			"didn't"
		);

		assertThat(thrown.getMessage(), containsString("not a proper UniProt Display Name"));
	}

	@Test
	public void emptyUniProtToReactomeEventsMapFromEmptyDatabase() {
		DummyGraphDBServer dummyGraphDBServer = DummyGraphDBServer.getInstance();
		dummyGraphDBServer.initializeNeo4j();

		Map<UniProtReactomeEntry, Set<ReactomeEvent>> uniProtToReactomeEvents =
			UniProtReactomeEntry.fetchUniProtReactomeEntryToReactomeEvents(dummyGraphDBServer.getSession());

		assertThat(uniProtToReactomeEvents, is(anEmptyMap()));
	}

	@Test
	public void correctUniProtToReactomeEventsMapRetrieved() {
		final long EVENT_DB_ID = 1640170L;
		final String EVENT_DISPLAY_NAME = "Cell Cycle";
		final String EVENT_STABLE_ID = "R-HSA-1640170";

		final int NUMBER_OF_EXPECTED_UNIPROT_REACTOME_ENTRIES = 2;
		final int NUMBER_OF_EXPECTED_REACTOME_EVENTS = 7;

		Map<UniProtReactomeEntry, Set<ReactomeEvent>> uniProtToReactomeEvents =
			UniProtReactomeEntry.fetchUniProtReactomeEntryToReactomeEvents(
				getPopulatedDummyGraphDBServer().getSession()
			);

		assertThat(uniProtToReactomeEvents, is(aMapWithSize(NUMBER_OF_EXPECTED_UNIPROT_REACTOME_ENTRIES)));

		Set<ReactomeEvent> eventsAttachedToUniProtInstance = uniProtToReactomeEvents.get(canonicalUniProtReactomeEntry);
		assertThat(eventsAttachedToUniProtInstance, hasSize(NUMBER_OF_EXPECTED_REACTOME_EVENTS));

		ReactomeEvent expectedEvent = new ReactomeEvent(EVENT_DB_ID, EVENT_DISPLAY_NAME, EVENT_STABLE_ID);
		assertThat(eventsAttachedToUniProtInstance, hasItem(expectedEvent));
	}

	@Test
	public void emptyUniProtToTopLevelPathwaysMapFromEmptyDatabase() {
		DummyGraphDBServer dummyGraphDBServer = DummyGraphDBServer.getInstance();
		dummyGraphDBServer.initializeNeo4j();

		Map<UniProtReactomeEntry, Set<ReactomeEvent>> uniProtToTopLevelPathways =
			UniProtReactomeEntry.fetchUniProtReactomeEntryToTopLevelPathways(dummyGraphDBServer.getSession());

		assertThat(uniProtToTopLevelPathways, is(anEmptyMap()));
	}

	@Test
	public void correctUniProtToTopLevelPathways() {
		final long TOP_LEVEL_PATHWAY_DB_ID = 162582L;
		final String TOP_LEVEL_PATHWAY_DISPLAY_NAME = "Signal Transduction";
		final String TOP_LEVEL_PATHWAY_STABLE_ID = "R-HSA-162582";

		final int NUMBER_OF_EXPECTED_UNIPROT_REACTOME_ENTRIES = 2;
		final int NUMBER_OF_EXPECTED_TOP_LEVEL_PATHWAYS = 1;

		Map<UniProtReactomeEntry, Set<ReactomeEvent>> uniProtToTopLevelPathways =
			UniProtReactomeEntry.fetchUniProtReactomeEntryToTopLevelPathways(
				getPopulatedDummyGraphDBServer().getSession()
			);

		assertThat(uniProtToTopLevelPathways, is(aMapWithSize(NUMBER_OF_EXPECTED_UNIPROT_REACTOME_ENTRIES)));

		Set<ReactomeEvent> eventsAttachedToUniProtInstance = uniProtToTopLevelPathways.get(isoformUniProtReactomeEntry);
		assertThat(eventsAttachedToUniProtInstance, hasSize(NUMBER_OF_EXPECTED_TOP_LEVEL_PATHWAYS));

		ReactomeEvent expectedEvent = new ReactomeEvent(
			TOP_LEVEL_PATHWAY_DB_ID, TOP_LEVEL_PATHWAY_DISPLAY_NAME, TOP_LEVEL_PATHWAY_STABLE_ID
		);
		assertThat(eventsAttachedToUniProtInstance, contains(expectedEvent));
	}

	private DummyGraphDBServer getPopulatedDummyGraphDBServer() {
		DummyGraphDBServer dummyGraphDBServer = DummyGraphDBServer.getInstance();
		dummyGraphDBServer.initializeNeo4j();
		dummyGraphDBServer.populateDummyGraphDB();

		return dummyGraphDBServer;
	}
}