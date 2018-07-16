package org.reactome.release.uniprotupdate.dataschema;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="entry")
public class UniprotData
{

	
	private List<String> accessions;
	private List<Chain> chains;
	private List<Gene> genes;
	private List<Isoform> isoforms;
	private List<Keyword> keywords;
	private List<String> ensembleGeneIDs;
	private String sequenceLength;
	private String sequenceChecksum;
	private String scientificName;
	private String recommendedName;
	
	@XmlElement(name="accession")
	public List<String> getAccessions()
	{
		return accessions;
	}

	public void setAccessions(List<String> accessions)
	{
		this.accessions = accessions;
	}
	
	@XmlElement(name="chain")
	public List<Chain> getChains()
	{
		return chains;
	}

	public void setChains(List<Chain> chains)
	{
		this.chains = chains;
	}

	@XmlElement(name="gene")
	public List<Gene> getGenes()
	{
		return genes;
	}

	public void setGenes(List<Gene> genes)
	{
		this.genes = genes;
	}

	@XmlElement(name="isoform")
	public List<Isoform> getIsoforms()
	{
		return isoforms;
	}

	public void setIsoforms(List<Isoform> isoforms)
	{
		this.isoforms = isoforms;
	}

	@XmlElement(name="keyword")
	public List<Keyword> getKeywords()
	{
		return keywords;
	}

	public void setKeywords(List<Keyword> keywords)
	{
		this.keywords = keywords;
	}

	@XmlElement
	public String getSequenceLength()
	{
		return sequenceLength;
	}

	public void setSequenceLength(String sequenceLength)
	{
		this.sequenceLength = sequenceLength;
	}

	@XmlElement
	public String getSequenceChecksum()
	{
		return sequenceChecksum;
	}

	public void setSequenceChecksum(String sequenceChecksum)
	{
		this.sequenceChecksum = sequenceChecksum;
	}

	@XmlElement
	public String getScientificName()
	{
		return scientificName;
	}

	public void setScientificName(String scientificName)
	{
		this.scientificName = scientificName;
	}

	@XmlElement
	public String getRecommendedName()
	{
		return recommendedName;
	}

	public void setRecommendedName(String recommendedName)
	{
		this.recommendedName = recommendedName;
	}

	@XmlElement(name="ensemblGeneID")
	public List<String> getEnsembleGeneIDs() {
		return ensembleGeneIDs;
	}

	public void setEnsembleGeneIDs(List<String> ensembleGeneIDs) {
		this.ensembleGeneIDs = ensembleGeneIDs;
	}	
}
