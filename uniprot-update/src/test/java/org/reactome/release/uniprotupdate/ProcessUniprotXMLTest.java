package org.reactome.release.uniprotupdate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

@TestInstance(Lifecycle.PER_CLASS)
public class ProcessUniprotXMLTest {
	private List<UniprotData> uniprotEntries;
	private UniprotData representativeUniProtEntry;

	@BeforeAll
	public void retrieveUniProtEntries()
		throws IOException, JAXBException, XMLStreamException {
		String uniprotKRASFile = this.getClass().getClassLoader().getResource("UniProt_sample_entries.xml").getPath();

		this.uniprotEntries = ProcessUniprotXML.getDataFromUniprotFile(uniprotKRASFile, true);
		this.representativeUniProtEntry = this.uniprotEntries.get(0);
	}

	@Test
	public void correctNumberOfExpectedUniProtEntries() {
		final int EXPECTED_NUMBER_OF_UNIPROT_ENTRIES = 3;

		assertThat(this.uniprotEntries.size(), is(equalTo(EXPECTED_NUMBER_OF_UNIPROT_ENTRIES)));
	}

	@Test
	public void nameIsCorrectInUniProtEntry() {
		final String EXPECTED_NAME = "RASK_HUMAN";

		assertThat(this.representativeUniProtEntry.getName(), is(equalTo(EXPECTED_NAME)));
	}

	@Test
	public void organismNameIsCorrectInUniProtEntry() {
		final String EXPECTED_ORGANISM_NAME = "Homo sapiens";

		assertThat(this.representativeUniProtEntry.getOrganismName(), is(equalTo(EXPECTED_ORGANISM_NAME)));
	}

	@Test
	public void recommendedNameIsCorrectInUniProtEntry() {
		final String EXPECTED_RECOMMENDED_NAME = "recommendedName: GTPase KRas";

		assertThat(this.representativeUniProtEntry.getRecommendedFullName(), is(equalTo(EXPECTED_RECOMMENDED_NAME)));
	}

	@Test
	public void recommendedShortNameIsCorrectInUniProtEntry() {
		UniprotData uniprotEntryWithRecommendedShortName = this.uniprotEntries.get(1);
		final String EXPECTED_RECOMMENDED_SHORT_NAME = "shortName: E2F-8";

		assertThat(
			uniprotEntryWithRecommendedShortName.getRecommendedShortName(),
			is(equalTo(EXPECTED_RECOMMENDED_SHORT_NAME))
		);
	}

	@Test
	public void alternativeNamesAreCorrectInUniProtEntry() {
		assertThat(
			this.representativeUniProtEntry.getAlternativeNamesAsStrings(),
			contains(
				"alternativeName: K-Ras 2",
				"alternativeName: Ki-Ras",
				"alternativeName: c-K-ras",
				"alternativeName: c-Ki-ras"
			)
		);
	}

	@Test
	public void alternativeShortNameIsCorrectInUniProtEntry() {
		UniprotData uniprotEntryWithAlternativeNameShortName = uniprotEntries.get(2);
		final String EXPECTED_ALTERNATIVE_NAME_SHORT_NAME = "shortName: SSc5D";

		assertThat(
			uniprotEntryWithAlternativeNameShortName.getAlternativeShortNames(),
			contains(EXPECTED_ALTERNATIVE_NAME_SHORT_NAME)
		);
	}

	@Test
	public void accessionsAreCorrectInUniProtEntry() {
		assertThat(
			this.representativeUniProtEntry.getAccessions(),
			contains(
				"P01116",
				"A8K8Z5",
				"B0LPF9",
				"P01118",
				"Q96D10"
			)
		);
	}

	@Test
	public void primaryAccessionIsCorrectInUniProtEntry() {
		final String EXPECTED_PRIMARY_ACCESSION = "P01116";

		assertThat(this.representativeUniProtEntry.getPrimaryAccession(), is(equalTo(EXPECTED_PRIMARY_ACCESSION)));
	}

	@Test
	public void secondaryAccessionsAreCorrectInUniProtEntry() {
		assertThat(
			this.representativeUniProtEntry.getSecondaryAccessions(),
			contains(
				"A8K8Z5",
				"B0LPF9",
				"P01118",
				"Q96D10"
			)
		);
	}

	@Test
	public void flattenedGeneNamesAreCorrectInUniProtEntry() {
		assertThat(
			this.representativeUniProtEntry.getFlattenedGeneNames(),
			contains(
				"KRAS",
				"KRAS2",
				"RASK2"
			)
		);
	}

	@Test
	public void ensemblGeneIdsAreCorrectInUniProtEntry() {
		final String EXPECTED_GENE_ID = "ENSG00000133703";

		assertThat(
			this.representativeUniProtEntry.getUniqueEnsEMBLGeneIds(),
			contains(EXPECTED_GENE_ID)
		);
	}

	@Test
	public void chainsAreCorrectInUniProtEntry() {
		assertThat(
			this.representativeUniProtEntry.getChains(),
			contains(
				"initiator methionine:1",
				"chain:1-186",
				"chain:2-186",
				"propeptide:187-189"
			)
		);
	}

	@Test
	public void positionalChainsAreCorrectInUniProtEntry() {
		final String EXPECTED_POSITIONAL_CHAIN = "initiator methionine:1";

		assertThat(
			this.representativeUniProtEntry.getPositionalChains(),
			contains(EXPECTED_POSITIONAL_CHAIN)
		);
	}

	@Test
	public void rangedChainsAreCorrectInUniProtEntry() {
		assertThat(
			this.representativeUniProtEntry.getRangedChains(),
			contains(
				"chain:1-186",
				"chain:2-186",
				"propeptide:187-189"
			)
		);
	}

	@Test
	public void flattenedCommentTextIsCorrectInUniProtEntry() {
		assertThat(
			this.representativeUniProtEntry.getCommentTexts(),
			contains(
				"FUNCTION Ras proteins bind GDP/GTP and possess intrinsic GTPase activity. Plays an important role " +
				"in the regulation of cell proliferation (PubMed:23698361, PubMed:22711838). Plays a role in " +
				"promoting oncogenic events by inducing transcriptional silencing of tumor suppressor genes (TSGs) " +
				"in colorectal cancer (CRC) cells in a ZNF304-dependent manner (PubMed:24623306).",

				"ACTIVITY REGULATION Alternates between an inactive form bound to GDP and an active form bound to " +
				"GTP. Activated by a guanine nucleotide-exchange factor (GEF) and inactivated by a " +
				"GTPase-activating protein (GAP). Interaction with SOS1 promotes exchange of bound GDP by GTP.",

				"SUBUNIT Interacts with PHLPP. Interacts (active GTP-bound form preferentially) with RGS14 (By " +
				"similarity). Interacts (when farnesylated) with PDE6D; this promotes dissociation from the cell " +
				"membrane (PubMed:23698361). Interacts with SOS1 (PubMed:22431598).",

				"ALTERNATIVE PRODUCTS Isoforms differ in the C-terminal region which is encoded by two alternative " +
				"exons (IVA and IVB).",

				"PTM Acetylation at Lys-104 prevents interaction with guanine nucleotide exchange factors (GEFs).",

				"PTM Ubiquitinated by the BCR(LZTR1) E3 ubiquitin ligase complex at Lys-170 in a non-degradative " +
				"manner, leading to inhibit Ras signaling by decreasing Ras association with membranes.",

				"DISEASE The disease is caused by mutations affecting the gene represented in this entry.",

				"DISEASE The disease is caused by mutations affecting the gene represented in this entry.",

				"DISEASE The disease is caused by mutations affecting the gene represented in this entry.",

				"DISEASE The disease is caused by mutations affecting the gene represented in this entry.",

				"DISEASE Defects in KRAS are a cause of pylocytic astrocytoma (PA). Pylocytic astrocytomas are " +
				"neoplasms of the brain and spinal cord derived from glial cells which vary from histologically " +
				"benign forms to highly anaplastic and malignant tumors.",

				"DISEASE The disease is caused by mutations affecting the gene represented in this entry.",

				"DISEASE KRAS mutations are involved in cancer development.",

				"SIMILARITY Belongs to the small GTPase superfamily. Ras family.",

				"ONLINE INFORMATION The Singapore human mutation and polymorphism database"
			)
		);
	}

	@Test
	public void flattenedKeywordsAreCorrectInUniProtEntry() {
		assertThat(
			this.representativeUniProtEntry.getFlattenedKeywords(),
			contains(
				"3D-structure",
				"Acetylation",
				"Alternative splicing",
				"Cardiomyopathy",
				"Cell membrane",
				"Complete proteome",
				"Cytoplasm",
				"Deafness",
				"Direct protein sequencing",
				"Disease mutation",
				"Ectodermal dysplasia",
				"GTP-binding",
				"Isopeptide bond",
				"Lipoprotein",
				"Membrane",
				"Mental retardation",
				"Methylation",
				"Nucleotide-binding",
				"Palmitate",
				"Polymorphism",
				"Prenylation",
				"Proto-oncogene",
				"Reference proteome",
				"Ubl conjugation"
			)
		);
	}

	@Test
	public void sequenceChecksumIsCorrectInUniProtEntry() {
		final String EXPECTED_CHECKSUM_VALUE = "973547B2E11C2C81";

		assertThat(this.representativeUniProtEntry.getSequenceChecksum(), is(equalTo(EXPECTED_CHECKSUM_VALUE)));
	}

	@Test
	public void sequenceLengthIsCorrectInUniProtEntry() {
		final int EXPECTED_SEQUENCE_LENGTH = 189;

		assertThat(this.representativeUniProtEntry.getSequenceLength(), is(equalTo(EXPECTED_SEQUENCE_LENGTH)));
	}

	@Test
	public void isoformsAreCorrectInUniProtEntry() {
		assertThat(
			this.representativeUniProtEntry.getIsoforms(),
			contains(
				"P01116-1",
				"P01116-2"
			)
		);
	}
}
