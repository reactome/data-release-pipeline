package org.reactome.release.uniprotupdate;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceEdit;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchemaAttribute;
import org.reactome.release.uniprotupdate.dataschema.Gene;
import org.reactome.release.uniprotupdate.dataschema.Name;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

/**
 * 
 * @author sshorser
 *
 */
public class UniprotUpdater
{

	private static final String ENSEMBL_HOMO_SAPIENS_GENE = "ENSEMBL_Homo_sapiens_GENE";
	private static final String HOMO_SAPIENS = "Homo sapiens";
	// List of species names was taken from uniprot_xml2sql_isoform.pl:84
	private static final Set<String> speciesToUpdate = new HashSet<String>( Arrays.asList(HOMO_SAPIENS, "Mus musculus", "Rattus norvegicus",
																			"Bos taurus", "Gallus gallus", "Drosophila melanogaster",
																			"Caenorhabditis elegans", "Saccharomyces cerevisiae", "Schizosaccharomyces pombe",
																			"Human immunodeficiency virus type 1", "Human immunodeficiency virus type 2", "Influenza A virus") );
	
	private static GKInstance ensemblHSapiensRefDB;
	private static GKInstance humanSpecies;
	
	private static String geneNamesListToString(List<Gene> geneNames)
	{
		StringBuilder sb = new StringBuilder();
		for (Gene gene : geneNames)
		{
			for (Name name : gene.getNames())
			{
				sb.append("\"").append(name.getValue()).append("\", ");
			}
			
		}
		return sb.toString();
	}
	
	/**
	 * Updates UniProt instances.
	 * @param uniprotData - The uniprot data that was extracted from the XML file. This will be a list of UniprotData objects, each object representing an &lt;entry/&gt; entity from the file.
	 * @param referenceDNASequences - A map of ReferenceDNASequence objects, keyed by their Identifier (ReferenceDNASequences without an identifier should not be in this list).
	 * @throws Exception 
	 */
	public void updateUniprotInstances(MySQLAdaptor adaptor, List<UniprotData> uniprotData, Map<String, GKInstance> referenceDNASequences, InstanceEdit instanceEdit) throws Exception
	{
		synchronized (UniprotUpdater.ensemblHSapiensRefDB)
		{
			// initialize if it's null.
			if (UniprotUpdater.ensemblHSapiensRefDB == null)
			{
				@SuppressWarnings("unchecked")
				List<GKInstance> refDBs = (List<GKInstance>)adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase, ReactomeJavaConstants.name, "=", ENSEMBL_HOMO_SAPIENS_GENE);
				UniprotUpdater.ensemblHSapiensRefDB = refDBs.get(0);
			}
		}
		synchronized (UniprotUpdater.humanSpecies)
		{
			if (UniprotUpdater.humanSpecies == null)
			{
				@SuppressWarnings("unchecked")
				List<GKInstance> species = (List<GKInstance>) adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.species, ReactomeJavaConstants.name, "=", HOMO_SAPIENS);
				UniprotUpdater.humanSpecies = species.get(0);
			}
		}
		
		for (UniprotData data : uniprotData)
		{
			// first, let's make sure this piece of data is for a species that we can update via Uniprot Update.
			if (speciesToUpdate.contains(data.getScientificName()))
			{
				// for human data, we may need to update a ReferenceDNASequence.
				if (data.getScientificName().equals(HOMO_SAPIENS))
				{
					// Report when there are multiple gene names.
					if (data.getGenes().size() > 0)
					{
						System.out.println("Accession " + data.getAccessions().toString() + "multiple gene names: " + geneNamesListToString(data.getGenes()));
					}
					for (Gene geneName : data.getGenes())
					{
						for (Name name : geneName.getNames())
						{
							boolean modified = false;
							// Check to see if the ENSEMBL ID (Remember: the XSL only selects for "Ensembl" gene names) is in the list of ReferenceDNASequences.
							String geneNameFromFile = name.getValue();
							if (referenceDNASequences.containsKey(geneNameFromFile))
							{
								
								// If this instance already exists in the database, let's update it.
								GKInstance referenceDNASequence = referenceDNASequences.get(geneNameFromFile);
								GKInstance speciesFromDB = (GKInstance) referenceDNASequence.getAttributeValue(ReactomeJavaConstants.Species);
								@SuppressWarnings("unchecked")
								Set<String> speciesNamesFromDB = (Set<String>) speciesFromDB.getAttributeValuesList(ReactomeJavaConstants.name);
								// The old Perl code forces the species to be changed if the one in the database does not match the one in the file.
								if (!speciesNamesFromDB.contains(data.getScientificName()))
								{
									referenceDNASequence.setAttributeValue(ReactomeJavaConstants.species, humanSpecies);
									adaptor.updateInstanceAttribute(referenceDNASequence, ReactomeJavaConstants.species);
									modified = true;
								}
								
								@SuppressWarnings("unchecked")
								Set<String> geneNamesFromDB = (Set<String>)referenceDNASequence.getAttributeValuesList(ReactomeJavaConstants.geneName);
								// The old Perl code adds the geneName from the file, if it's not already in the database.
								if (!geneNamesFromDB.contains(geneNameFromFile))
								{
									referenceDNASequence.addAttributeValue(ReactomeJavaConstants.geneName, geneNameFromFile);
									adaptor.updateInstanceAttribute(referenceDNASequence, ReactomeJavaConstants.geneName);
									modified = true;
								}
								
								// The old Perl code sets the reference database if it's not ENSEMBL_Homo_sapiens_GENE
								if (!((String)((GKInstance)referenceDNASequence.getAttributeValue(ReactomeJavaConstants.referenceDatabase)).getAttributeValue(ReactomeJavaConstants.name)).equals(UniprotUpdater.ENSEMBL_HOMO_SAPIENS_GENE) )
								{
									referenceDNASequence.setAttributeValue(ReactomeJavaConstants.referenceDatabase, UniprotUpdater.ensemblHSapiensRefDB);
									adaptor.updateInstanceAttribute(referenceDNASequence, ReactomeJavaConstants.referenceDatabase);
									modified = true;
								}
								// if the instance was modified, attach a new InstanceEdit to the modified attribute.
								if (modified)
								{
									referenceDNASequence.getAttributeValuesList(ReactomeJavaConstants.modified);
									referenceDNASequence.addAttributeValue(ReactomeJavaConstants.modified, instanceEdit);
									adaptor.updateInstanceAttribute(referenceDNASequence, ReactomeJavaConstants.modified);
								}
							}
							// if the gene ID was NOT in the ReferenceDNASequences map, we may need to add it to the database.
							else
							{
								GKInstance newRefDNASequence = new GKInstance(adaptor.getSchema().getClassByName(ReactomeJavaConstants.ReferenceDNASequence));
								newRefDNASequence.setAttributeValue(ReactomeJavaConstants.geneName, geneNameFromFile);
								newRefDNASequence.setAttributeValue(ReactomeJavaConstants.species, humanSpecies);
								newRefDNASequence.setAttributeValue(ReactomeJavaConstants.referenceDatabase, UniprotUpdater.ensemblHSapiensRefDB);
								newRefDNASequence.setAttributeValue(ReactomeJavaConstants.identifier, "???");
								newRefDNASequence.setAttributeValue(ReactomeJavaConstants.created, instanceEdit);
								
							}
						}
					}
				}
				// Process the rest of the data - chains, isoforms...
			}
		}
	}
}
