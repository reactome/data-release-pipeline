package org.reactome.release.dataexport;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class UCSCTest {
	private final String DUMMY_DIR = "outputDir";
	private final int DUMMY_REACTOME_VERSION = 999;


	@Test
	public void noUniProtReactomeEntriesRetrievedFromEmptyDatabase() {

		DummyGraphDBServer dummyGraphDBServer = DummyGraphDBServer.getInstance();
		dummyGraphDBServer.initializeNeo4j();

		Set<UniProtReactomeEntry> uniProtReactomeEntries = UCSC.getInstance(DUMMY_DIR, DUMMY_REACTOME_VERSION)
			.getUniProtReactomeEntriesForUCSC(dummyGraphDBServer.getSession());

		assertThat(uniProtReactomeEntries, is(empty()));
	}

	@Test
	public void correctCanonicalUniProtReactomeEntriesRetrieved() {
		final long UNIPROT_DB_ID = 69487L;
		final String UNIPROT_ACCESSION = "P04637";
		final String UNIPROT_DISPLAY_NAME = "UniProt:P04637 TP53";

		DummyGraphDBServer dummyGraphDBServer = DummyGraphDBServer.getInstance();
		dummyGraphDBServer.initializeNeo4j();
		dummyGraphDBServer.populateDummyGraphDB();

		Set<UniProtReactomeEntry> uniProtReactomeEntries = UCSC.getInstance(DUMMY_DIR, DUMMY_REACTOME_VERSION)
			.getUniProtReactomeEntriesForUCSC(dummyGraphDBServer.getSession());

		UniProtReactomeEntry expectedUniProtReactomeEntry =
			UniProtReactomeEntry.get(UNIPROT_DB_ID, UNIPROT_ACCESSION, UNIPROT_DISPLAY_NAME);

		assertThat(uniProtReactomeEntries, hasItem(expectedUniProtReactomeEntry));
	}

	@Test
	public void correctIsoformUniProtReactomeEntriesRetrieved() {
		final long UNIPROT_DB_ID = 152926L;
		final String UNIPROT_ACCESSION = "O75916-3";
		final String UNIPROT_DISPLAY_NAME = "UniProt:O75916-3 RGS9";

		DummyGraphDBServer dummyGraphDBServer = DummyGraphDBServer.getInstance();
		dummyGraphDBServer.initializeNeo4j();
		dummyGraphDBServer.populateDummyGraphDB();

		Set<UniProtReactomeEntry> uniProtReactomeEntries = UCSC.getInstance(DUMMY_DIR, DUMMY_REACTOME_VERSION)
			.getUniProtReactomeEntriesForUCSC(dummyGraphDBServer.getSession());

		UniProtReactomeEntry expectedUniProtReactomeEntry =
			UniProtReactomeEntry.get(UNIPROT_DB_ID, UNIPROT_ACCESSION, UNIPROT_DISPLAY_NAME);

		assertThat(uniProtReactomeEntries, hasItem(expectedUniProtReactomeEntry));
	}
}
