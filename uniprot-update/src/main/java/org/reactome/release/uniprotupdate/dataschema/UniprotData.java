package org.reactome.release.uniprotupdate.dataschema;

import static org.reactome.release.uniprotupdate.dataschema.GeneName.primaryNamesFirst;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.reactome.release.uniprotupdate.dataschema.chain.PositionalSequenceChain;
import org.reactome.release.uniprotupdate.dataschema.chain.RangedSequenceChain;

/**
 * Class representing a UniProt entry
 * @author sshorser, jweiser
 */
@XmlRootElement(name="entry")
@XmlType(name="entry", namespace="http://uniprot.org/uniprot")
@XmlAccessorType(XmlAccessType.FIELD)
public class UniprotData {
	@XmlElement(name="accession")
	private List<String> accessions;

	@XmlElement(name="name")
	private String name;

	@XmlElement(name="positionalChain")
	private List<PositionalSequenceChain> positionalChains;

	@XmlElement(name="rangedChain")
	private List<RangedSequenceChain> rangedChains;

	@XmlElement(name="gene")
	private List<Gene> genes;

	@XmlElement(name="isoform")
	private List<Isoform> isoforms;

	@XmlElement(name="keyword")
	private List<Keyword> keywords;

	@XmlElement(name="ensemblGeneID")
	private List<String> ensembleGeneIDs;

	@XmlElement(name="commentText")
	private List<CommentText> commentTexts;

	@XmlElement
	private int sequenceLength;

	@XmlElement
	private String sequenceChecksum;

	@XmlElement
	private String organismName;

	@XmlElement
	private RecommendedName recommendedName;

	@XmlElement(name="alternativeName")
	private List<AlternativeName> alternativeNames;

	/**
	 * Returns the primary UniProt accession
	 * @return Primary UniProt acccession or an empty String if there is none
	 */
	public String getPrimaryAccession() {
		return !getAccessions().isEmpty() ? getAccessions().get(0) : "";
	}

	/**
	 * Returns a list of all secondary UniProt accessions for this UniProt entry object, if any
	 * @return List of secondary UniProt accessions or an empty list if there are none
	 */
	public List<String> getSecondaryAccessions() {
		return getAccessions().size() > 1 ?
			getAccessions().subList(1, getAccessions().size()) :
			new ArrayList<>();
	}

	/**
	 * Returns a list of all UniProt accessions for this Uniprot entry object
	 * @return List of UniProt accessions or an empty list if there are none
	 */
	public List<String> getAccessions() {
		if (accessions == null) {
			return new ArrayList<>();
		}

		return accessions;
	}

	/**
	 * Sets the list of UniProt accessions for this UniProt entry object
	 * @param accessions List of UniProt accessions to assign to this UniProt entry object
	 */
	public void setAccessions(List<String> accessions) {
		this.accessions = accessions;
	}

	/**
	 * Returns the name of this UniProt entry object
	 * @return Name of the UniProt entry object or an empty String if there is none
	 */
	public String getName() {
		if (name == null) {
			return "";
		}

		return name;
	}

	/**
	 * Sets the name of the UniProt entry object
	 * @param name Name to assign to the UniProt entry object
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns a list of all chains for this UniProt entry object
	 * @return List of Strings representing chains (e.g. "peptide:1-15") or an empty list if there are none
	 */
	public List<String> getChains() {
		List<String> chains = new ArrayList<>();
		chains.addAll(getPositionalChains());
		chains.addAll(getRangedChains());

		return chains;
	}

	/**
	 * Returns a list of all positional chains (i.e. a single amino acid) for this UniProt entry object
	 * @return List of Strings representing positional chains (e.g. "initiator methionine:1") or an empty list if there
	 * are none
	 */
	public List<String> getPositionalChains() {
		if (positionalChains == null) {
			return new ArrayList<>();
		}

		return positionalChains
			.stream()
			.map(PositionalSequenceChain::toString)
			.collect(Collectors.toList());
	}

	/**
	 * Returns a list of all ranged chains (i.e. a consecutive sequence of several amino acids) for this UniProt entry
	 * object
	 * @return List of Strings representing ranged chains (e.g. "peptide:1-15") or an empty list if there
	 * are none
	 */
	public List<String> getRangedChains() {
		if (rangedChains == null) {
			return new ArrayList<>();
		}

		return rangedChains
			.stream()
			.map(RangedSequenceChain::toString)
			.collect(Collectors.toList());
	}

	/**
	 * Sets the list of UniProt chains describing a specific sequence position (i.e. one amino acid) for this UniProt
	 * entry object
	 * @param positionalChains List of UniProt positional sequence chains to assign to this UniProt entry object
	 */
	public void setPositionalChains(List<PositionalSequenceChain> positionalChains) {
		this.positionalChains = positionalChains;
	}

	/**
	 * Sets the list of UniProt chains describing a sequence range for this UniProt entry object
	 * @param rangedChains List of UniProt ranged sequence chains to assign to this UniProt entry object
	 */
	public void setRangedChains(List<RangedSequenceChain> rangedChains) {
		this.rangedChains = rangedChains;
	}

	/**
	 * Returns a list of all genes associated with this UniProt entry object
	 * @return List of Gene objects or an empty list if there are none
	 */
	@SuppressWarnings("unchecked")
	public List<Gene> getGenes() {
		if (genes == null) {
			return new ArrayList<>();
			//return Collections.unmodifiableList(Collections.EMPTY_LIST);
		}

		// Don't allow the collection to be modified, since that would have
		// an impact on the flattened gene list.
		return genes;
		//return Collections.unmodifiableList(genes);
	}

	/**
	 * Sets the list of genes for this UniProt entry object
	 * @param genes List of Gene objects to assign to this UniProt entry object
	 */
	public void setGenes(List<Gene> genes) {
		this.genes = genes;
		// Only execute the gene name-flattening code when the genes list is actually set.
		// This is the only place where the data structures that underly flattenedGeneNames
		// can be modified.

	}

	/**
	 * Returns a list of all isoforms associated with this UniProt entry object
	 * @return List of Strings representing isoforms (e.g. "P01116-1") or an empty list if there are none
	 */
	public List<String> getIsoforms() {
		if (isoforms == null) {
			return new ArrayList<>();
		}

		return isoforms
			.stream()
			.map(Isoform::toString)
			.collect(Collectors.toList());
	}

	/**
	 * Sets the list of UniProt isoforms for this UniProt entry object
	 * @param isoforms List of UniProt isoforms to assign to this UniProt entry object
	 */
	public void setIsoforms(List<Isoform> isoforms) {
		this.isoforms = isoforms;
	}

	/**
	 * Returns a list of all keywords associated with this UniProt entry object
	 * @return List of Keyword objects or an empty list if there are none
	 */
	public List<Keyword> getKeywords() {
		if (keywords == null) {
			return new ArrayList<>();
		}

		return keywords;
	}

	/**
	 * Sets the list of keywords for this UniProt entry object
	 * @param keywords List of keywords to assign to this UniProt entry object
	 */
	public void setKeywords(List<Keyword> keywords) {
		this.keywords = keywords;
	}

	/**
	 * Returns the sequence length associated with this UniProt entry object
	 * @return Sequence length
	 */
	public int getSequenceLength() {
		return sequenceLength;
	}

	/**
	 * Sets the sequence length for this UniProt entry object
	 * @param sequenceLength Sequence length to assign to this UniProt entry object
	 */
	public void setSequenceLength(int sequenceLength) {
		this.sequenceLength = sequenceLength;
	}

	/**
	 * Returns the sequence checksum associated with this UniProt entry object
	 * @return Sequence checksum or an empty String if there is none
	 */
	public String getSequenceChecksum() {
		return sequenceChecksum != null ? sequenceChecksum : "";
	}

	/**
	 * Sets the sequence checksum for this UniProt entry object
	 * @param sequenceChecksum Sequence checksum to assign to this UniProt entry object
	 */
	public void setSequenceChecksum(String sequenceChecksum) {
		this.sequenceChecksum = sequenceChecksum;
	}

	/**
	 * Returns the scientific organism name associated with this UniProt entry object
	 * @return Scientific organism name or an empty String if there is none
	 */
	public String getOrganismName() {
		return organismName != null ? organismName : "";
	}

	/**
	 * Sets the scientific organism name for this UniProt entry object
	 * @param organismName Scientific organism name to assign to this UniProt entry object
	 */
	public void setScientificName(String organismName) {
		this.organismName = organismName;
	}

	/**
	 * Returns the recommended name object associated with this UniProt entry object
	 * @return Recommended name or null if there is none
	 */
	public RecommendedName getRecommendedName() {
		return recommendedName;
	}

	/**
	 * Returns the recommended name in full (i.e. not shortened/abbreviated) associated with this UniProt entry object
	 * @return Full recommended name or empty String if there is none
	 */
	public String getRecommendedFullName() {
		return getRecommendedName() != null ? getRecommendedName().getFullName() : "";
	}

	/**
	 * Returns the short recommended name (i.e. not shortened/abbreviated) associated with this UniProt entry object
	 * @return Short recommended name or empty String if there is none
	 */
	public String getRecommendedShortName() {
		return getRecommendedName() != null ? getRecommendedName().getShortName() : "";
	}

	/**
	 * Sets the recommended name for this UniProt entry object
	 * @param recommendedName Recommended name to assign to this UniProt entry object
	 */
	public void setRecommendedName(RecommendedName recommendedName) {
		this.recommendedName = recommendedName;
	}

	/**
	 * Returns a list of all distinct EnsEMBL Gene IDs associated with this UniProt entry object
	 * @return Set of distinct EnsEMBL Gene IDs Strings or an empty list if there are none
	 */
	public Set<String> getUniqueEnsEMBLGeneIds() {
		return new HashSet<>(getEnsembleGeneIDs());
	}

	/**
	 * Returns a list of all EnsEMBL Gene IDs associated with this UniProt entry object
	 * @return List of EnsEMBL Gene Id Strings or an empty list if there are none
	 */
	public List<String> getEnsembleGeneIDs() {
		if (ensembleGeneIDs == null) {
			return new ArrayList<>();
		}

		return ensembleGeneIDs;
	}

	/**
	 * Sets the list of EnsEMBL Gene Ids for this UniProt entry object
	 * @param ensembleGeneIDs List of EnsEMBL Gene Id Strings to assign to this UniProt entry object
	 */
	public void setEnsembleGeneIDs(List<String> ensembleGeneIDs) {
		this.ensembleGeneIDs = ensembleGeneIDs;
	}

	/**
	 * Returns a list of all comments associated with this UniProt entry object
	 * @return List of Strings representing comment texts or an empty list if there are none
	 */
	public List<String> getCommentTexts() {
		if (commentTexts == null) {
			return new ArrayList<>();
		}

		return commentTexts.stream().map(CommentText::toString).collect(Collectors.toList());
	}

	/**
	 * Sets the list of all comments for this UniProt entry object
	 * @param commentText List of Strings representing comment texts to assign to this UniProt entry object
	 */
	public void setCommentTexts(List<CommentText> commentText) {
		this.commentTexts = commentText;
	}

	/**
	 * Returns a list of all gene names, as Strings, associated with this UniProt entry object
	 * @return List of gene name Strings or an empty list if there are none
	 */
	public List<String> getFlattenedGeneNames() {
		return getGenes()
			.stream()
			.flatMap(gene -> gene.getNames().stream())
			.sorted(primaryNamesFirst())
			.map(GeneName::getValue)
			.collect(Collectors.toList());
	}

	/**
	 * Returns all comments associated with this UniProt entry object, as a concatenated String
	 * @return All comments as a String or an empty String if there are none
	 */
	public String getFlattenedCommentsText() {
		if (this.commentTexts == null) {
			return "";
		}

		return this.commentTexts
			.stream()
			.map(CommentText::toString)
			.collect(Collectors.joining());

		// TODO: move the bulk of this code to the setter method for commentTexts?
	}

	/**
	 * Returns a list of all keywords, as Strings, associated with this UniProt entry object
	 * @return List of keyword Strings or an empty list if there are none
	 */
	public List<String> getFlattenedKeywords() {
		if (this.keywords == null) {
			return new ArrayList<>();
		}

		return this.keywords
			.stream()
			.map(Keyword::getKeyword)
			.collect(Collectors.toList());

		// TODO: move the bulk of this code to the setter method for keywords?
	}

	/**
	 * Returns a list of all alternative name objects associated with this UniProt entry object
	 * @return List of alternative name objects or an empty list if there are none
	 */
	public List<AlternativeName> getAlternativeNames() {
		if (alternativeNames == null) {
			return new ArrayList<>();
		}

		return alternativeNames;
	}

	/**
	 * Returns a list of all full alternative names, as Strings, associated with this UniProt entry object
	 * @return List of Strings representing alternative full names or an empty list if there are none
	 */
	public List<String> getAlternativeFullNames() {
		return getAlternativeNames().stream().map(AlternativeName::getFullName).collect(Collectors.toList());
	}

	/**
	 * Returns a list of all short alternative names, as Strings, associated with this UniProt entry object
	 * @return List of Strings representing alternative short names or an empty list if there are none
	 */
	public List<String> getAlternativeShortNames() {
		return getAlternativeNames().stream().map(AlternativeName::getShortName).collect(Collectors.toList());
	}

	/**
	 * Returns a list of all alternative names, as Strings, associated with this UniProt entry object.  Each String in
	 * the list gives the full and alternative name together.
	 * @return List of Strings representing alternative full names or an empty list if there are none
	 */
	public List<String> getAlternativeNamesAsStrings() {
		return getAlternativeNames().stream().map(AlternativeName::toString).collect(Collectors.toList());
	}

	/**
	 * Sets the list of alternative name objects for this UniProt entry object
	 * @param alternativeNames List of alternative name objects to assign to this UniProt entry object
	 */
	public void setAlternativeNames(List<AlternativeName> alternativeNames) {
		this.alternativeNames = alternativeNames;
	}

	/**
	 * Returns the String representation of this UniProt entry object including all unique data in the object
	 * @return String representation of this UniProt entry object
	 */
	@Override
	public String toString() {
		return String.join(System.lineSeparator(),
			"Primary Accession: " + this.getPrimaryAccession(),
			"Secondary Accessions: " + this.getSecondaryAccessions().toString(),
			"Name: " + this.getName(),
			"Recommended Name: " + this.getRecommendedName(),
			"Alternative Names: " + this.getAlternativeNames().toString(),
			"Organism Name: " + this.getOrganismName(),
			"Gene Names: " + this.getFlattenedGeneNames().toString(),
			"Sequence Length: " + this.getSequenceLength(),
			"Positional Sequence Chains: " + this.getPositionalChains().toString(),
			"Ranged Sequence Chains: " + this.getRangedChains().toString(),
			"Sequence Checksum: "  + this.getSequenceChecksum(),
			"Isoforms: " + this.getIsoforms().toString(),
			"Keywords: " + this.getFlattenedKeywords().toString(),
			"Comments: " + this.getFlattenedCommentsText()
		);
	}
}
