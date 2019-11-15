package org.reactome.release.uniprotupdate.dataschema;

import static org.reactome.release.uniprotupdate.dataschema.GeneName.primaryNamesFirst;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.reactome.release.uniprotupdate.dataschema.chain.SequenceChain;

/**
 * Class representing a UniProt entry
 * @author sshorser, jweiser
 */
@XmlRootElement(name="entry")
@XmlType(name="entry", namespace="https://uniprot.org/uniprot")
public class UniprotData {
	private List<String> accessions;
	private List<SequenceChain> chains;
	private List<Gene> genes;
	private List<Isoform> isoforms;
	private List<Keyword> keywords;
	private List<String> ensembleGeneIDs;
	private List<CommentText> commentTexts;
	private String sequenceLength;
	private String sequenceChecksum;
	private String scientificName;
	private String recommendedName;
	private List<String> alternativeNames;

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
	@XmlElement(name="accession")
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
	 * Returns a list of all chains for this UniProt entry object
	 * @return List of Chain objects or an empty list if there are none
	 */
	@XmlElement(name="chain")
	public List<SequenceChain> getChains() {
		if (chains == null) {
			return new ArrayList<>();
		}

		return chains;
	}

	/**
	 * Sets the list of UniProt chains for this UniProt entry object
	 * @param chains List of UniProt chains to assign to this UniProt entry object
	 */
	public void setChains(List<SequenceChain> chains) {
		this.chains = chains;
	}

	/**
	 * Returns a list of all genes associated with this UniProt entry object
	 * @return List of Gene objects or an empty list if there are none
	 */
	@XmlElement(name="gene")
	@SuppressWarnings("unchecked")
	public List<Gene> getGenes() {
		if (genes == null) {
			return Collections.unmodifiableList(Collections.EMPTY_LIST);
		}

		// Don't allow the collection to be modified, since that would have
		// an impact on the flattened gene list.
		return Collections.unmodifiableList(genes);
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
	 * @return List of Isoform objects or an empty list if there are none
	 */
	@XmlElement(name="isoform")
	public List<Isoform> getIsoforms() {
		if (isoforms == null) {
			return new ArrayList<>();
		}

		return isoforms;
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
	@XmlElement(name="keyword")
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
	 * @return Sequence length or an empty String if there is none
	 */
	@XmlElement
	public String getSequenceLength() {
		return sequenceLength != null ? sequenceLength : "";
	}

	/**
	 * Sets the sequence length for this UniProt entry object
	 * @param sequenceLength Sequence length to assign to this UniProt entry object
	 */
	public void setSequenceLength(String sequenceLength) {
		this.sequenceLength = sequenceLength;
	}

	/**
	 * Returns the sequence checksum associated with this UniProt entry object
	 * @return Sequence checksum or an empty String if there is none
	 */
	@XmlElement
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
	 * Returns the scientific name associated with this UniProt entry object
	 * @return Scientific name or an empty String if there is none
	 */
	@XmlElement
	public String getScientificName() {
		return scientificName != null ? scientificName : "";
	}

	/**
	 * Sets the scientific name for this UniProt entry object
	 * @param scientificName Scientific name to assign to this UniProt entry object
	 */
	public void setScientificName(String scientificName) {
		this.scientificName = scientificName;
	}

	/**
	 * Returns the recommended name associated with this UniProt entry object
	 * @return Recommended name or an empty String if there is none
	 */
	@XmlElement
	public String getRecommendedName() {
		return recommendedName != null ? recommendedName : "";
	}

	/**
	 * Sets the recommended name for this UniProt entry object
	 * @param recommendedName Recommended name to assign to this UniProt entry object
	 */
	public void setRecommendedName(String recommendedName) {
		this.recommendedName = recommendedName;
	}

	/**
	 * Returns a list of all EnsEMBL Gene IDs associated with this UniProt entry object
	 * @return List of EnsEMBL Gene IDs objects or an empty list if there are none
	 */
	@XmlElement(name="ensemblGeneID")
	public List<String> getEnsembleGeneIDs() {
		if (ensembleGeneIDs == null) {
			return new ArrayList<>();
		}

		return ensembleGeneIDs;
	}

	/**
	 * Sets the list of EnsEMBL Gene Ids for this UniProt entry object
	 * @param ensembleGeneIDs List of EnsEMBL Gene Ids to assign to this UniProt entry object
	 */
	public void setEnsembleGeneIDs(List<String> ensembleGeneIDs) {
		this.ensembleGeneIDs = ensembleGeneIDs;
	}

	/**
	 * Returns a list of all comments associated with this UniProt entry object
	 * @return List of CommentText objects or an empty list if there are none
	 */
	@XmlElement(name="commentText")
	public List<CommentText> getCommentTexts() {
		if (commentTexts == null) {
			return new ArrayList<>();
		}

		return commentTexts;
	}

	/**
	 * Sets the list of comments for this UniProt entry object
	 * @param commentText List of comments to assign to this UniProt entry object
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
	 * Returns a list of all alternative names associated with this UniProt entry object
	 * @return List of alternative names or an empty list if there are none
	 */
	@XmlElement(name="alternativeName")
	public List<String> getAlternativeNames() {
		if (alternativeNames == null) {
			return new ArrayList<>();
		}

		return alternativeNames;
	}

	/**
	 * Sets the list of alternative names for this UniProt entry object
	 * @param alternativeNames List of alternative names to assign to this UniProt entry object
	 */
	public void setAlternativeNames(List<String> alternativeNames) {
		this.alternativeNames = alternativeNames;
	}
}
