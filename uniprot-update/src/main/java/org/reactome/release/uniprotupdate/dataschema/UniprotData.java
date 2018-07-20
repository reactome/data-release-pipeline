package org.reactome.release.uniprotupdate.dataschema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name="entry")
@XmlType(name="entry", namespace="http://uniprot.org/uniprot")
public class UniprotData
{

	private List<String> flattenedGeneNames = new ArrayList<String>();
	private String primaryGeneName;
	private List<String> accessions;
	private List<Chain> chains;
	private List<Gene> genes;
	private List<Isoform> isoforms;
	private List<Keyword> keywords;
	private List<String> ensembleGeneIDs;
	private List<CommentText> commentTexts;
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
		// Don't allow the collection to be modified, since that would have
		// an impact on the flattened gene list.
		return genes != null ? Collections.unmodifiableList(genes) : genes;
	}

	public void setGenes(List<Gene> genes)
	{
		this.genes = genes;
		// Only execute the gene name-flattening code when the genes list is actually set.
		// This is the only place where the data structures that underly flattenedGeneNames
		// can be modified.
		
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
	public List<String> getEnsembleGeneIDs()
	{
		return ensembleGeneIDs;
	}

	public void setEnsembleGeneIDs(List<String> ensembleGeneIDs)
	{
		this.ensembleGeneIDs = ensembleGeneIDs;
	}

	@XmlElement(name="commentText")
	public List<CommentText> getCommentTexts()
	{
		return commentTexts;
	}

	public void setCommentTexts(List<CommentText> commentText)
	{
		this.commentTexts = commentText;
	}	
	
	public List<String> getFlattenedGeneNames()
	{
		int i = 0;
		if (this.genes!=null)
		{
			for (Gene gene : this.getGenes())
			{
				for (Name name : gene.getNames())
				{
					this.flattenedGeneNames.add(name.getValue());
					if (name.getType().equals("primary"))
					{
						this.primaryGeneName = name.getValue();
					}
					else
					{
						// only have to count positions until after primary gene name has been found.
						if (this.primaryGeneName == null)
						{
							i ++;
						}
					}
				}
			}
		}
		// Move the primary gene name to the head of the array, if it's not already there.
		if (i>0 && this.primaryGeneName != null)
		{
			// remove primary gene name from its current position.
			this.flattenedGeneNames.remove(i);
			// add it at the begining.
			this.flattenedGeneNames.add(0, primaryGeneName);
		}
		
		return this.flattenedGeneNames;
	}
	
	public String getFlattenedCommentsText()
	{
		StringBuilder flattenedCommentsText = new StringBuilder();
		if (this.commentTexts!=null)
		{
			for (CommentText comment : this.commentTexts)
			{
				flattenedCommentsText.append(comment.getType().toUpperCase()).append(" ").append(comment.getText()).append(" ");
			}
		}
		// TODO: move the bulk of this code to the setter method for commentTexts?
		return flattenedCommentsText.toString().trim();
	}
	
	public List<String> getFlattenedKeywords()
	{
		List<String> flattenedKeywords = new ArrayList<String>();
		if (this.keywords!=null)
		{
			for (Keyword keyword : this.keywords)
			{
				flattenedKeywords.add(keyword.getKeyword());
			}
		}
		// TODO: move the bulk of this code to the setter method for keywords?
		return flattenedKeywords;
	}
}
