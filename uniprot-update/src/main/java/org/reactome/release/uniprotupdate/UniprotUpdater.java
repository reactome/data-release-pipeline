package org.reactome.release.uniprotupdate;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

/**
 * This class is to be used to update Uniprot instances.
 * @author sshorser
 *
 */
class UniprotUpdater
{

	private static final String UNI_PROT = "UniProt";
	private static final Logger logger = LogManager.getLogger();
	
	static final String ENSEMBL_HOMO_SAPIENS_GENE = "ENSEMBL_Homo_sapiens_GENE";
	private static final String HOMO_SAPIENS = "Homo sapiens";
	// List of species names was taken from uniprot_xml2sql_isoform.pl:84 // TODO: move these values to a resource file.
	private static final Set<String> speciesToUpdate = new HashSet<>( Arrays.asList(HOMO_SAPIENS, "Mus musculus", "Rattus norvegicus",
																			"Bos taurus", "Gallus gallus", "Drosophila melanogaster",
																			"Caenorhabditis elegans", "Saccharomyces cerevisiae", "Schizosaccharomyces pombe",
																			"Human immunodeficiency virus type 1", "Human immunodeficiency virus type 2", "Influenza A virus") );
	
	static GKInstance ensemblHSapiensRefDB;
	static GKInstance uniprotRefDB;
	static GKInstance humanSpecies;

	/**
	 * Updates UniProt instances.
	 * @param uniprotData - The uniprot data that was extracted from the XML file. This will be a list of UniprotData objects, each object representing an &lt;entry/&gt; entity from the file.
	 * @param referenceDNASequences - A map of ReferenceDNASequence objects, keyed by their Identifier (ReferenceDNASequences without an identifier should not be in this list).
	 * @param referenceGeneProducts - A map of ReferenceGeneProduct objects, keyed by their Identifier (ReferenceGeneProduct without an identifier should not be in this list).
	 * @param referenceIsoforms - A map of ReferenceIsoforms objects, keyed by their VariantIdentifier (ReferenceIsoforms without an identifier should not be in this list).
	 * @throws Exception 
	 */
	void updateUniprotInstances(MySQLAdaptor adaptor, List<UniprotData> uniprotData, Map<String, GKInstance> referenceDNASequences, Map<String, GKInstance> referenceGeneProducts, Map<String, GKInstance> referenceIsoforms, GKInstance instanceEdit) throws Exception
	{
		synchronized (this)
		{
			// initialize if null.
			if (UniprotUpdater.ensemblHSapiensRefDB == null)
			{
				@SuppressWarnings("unchecked")
				List<GKInstance> refDBs = new ArrayList<>((Collection<GKInstance>) adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase, ReactomeJavaConstants.name, "=", ENSEMBL_HOMO_SAPIENS_GENE));
				UniprotUpdater.ensemblHSapiensRefDB = refDBs.get(0);
			}
			if (UniprotUpdater.humanSpecies == null)
			{
				@SuppressWarnings("unchecked")
				List<GKInstance> species = new ArrayList<>((Collection<GKInstance>) adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Species, ReactomeJavaConstants.name, "=", HOMO_SAPIENS));
				UniprotUpdater.humanSpecies = species.get(0);
			}
			if (UniprotUpdater.uniprotRefDB == null)
			{
				@SuppressWarnings("unchecked")
				List<GKInstance> refDBs = new ArrayList<>((Collection<GKInstance>) adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase, ReactomeJavaConstants.name, "=", UNI_PROT));
				UniprotUpdater.uniprotRefDB = refDBs.get(0);
			}
		}
		AtomicInteger totalEnsemblGeneCount = new AtomicInteger(0);
		Set<String> genesOKWithENSEMBL = Collections.synchronizedSet(new HashSet<String>());
		
		String ensemblGenesFileName = "ensemblGeneIDs.list";
		boolean usePreexistingEnsemblGeneList = true;
		// If the file already exists, load it into memory, into genesOKWithENSEMBL
		if (Files.exists(Paths.get(ensemblGenesFileName)) && usePreexistingEnsemblGeneList )
		{
			Files.readAllLines(Paths.get(ensemblGenesFileName)).parallelStream().forEach(line -> genesOKWithENSEMBL.add(line));
		}
//		int startingSize = genesOKWithENSEMBL.size();
		
		ENSEMBLQueryUtil.checkGenesWithENSEMBL(uniprotData, totalEnsemblGeneCount, genesOKWithENSEMBL, ensemblGenesFileName, HOMO_SAPIENS);

		Map<String, List<String>> secondaryAccessions = new HashMap<>();
		int i = 0;
		long startTime = System.currentTimeMillis();
		long totalUniprotRecords = 0;
		for (UniprotData data : uniprotData)
		{
			i++;
			long currentTime = System.currentTimeMillis();
			if ( TimeUnit.MILLISECONDS.toSeconds(currentTime - startTime) > 30 )
			{
				totalUniprotRecords += i;
				logger.info("{} uniprot records processed ", totalUniprotRecords);
				startTime = currentTime;
				i = 0;
			}
			// Should each pass through this loop be a single transaction? This might work well if this loop is run in parallel...
			// first, let's make sure this piece of data is for a species that we can update via Uniprot Update.
			if (UniprotUpdater.speciesToUpdate.contains(data.getScientificName()))
			{
				List<String> geneList = new ArrayList<>();
				if (data.getEnsembleGeneIDs()!=null)
				{
					geneList = data.getEnsembleGeneIDs().stream().distinct().collect(Collectors.toList());
				}
//				List<GKInstance> referenceDNASequencesForThisUniprot = new ArrayList<>(geneList.size());
				
				// Update secondary accessions
				String accession = data.getAccessions().get(0);
				if (secondaryAccessions.containsKey(accession))
				{
					secondaryAccessions.get(accession).addAll(data.getAccessions().subList(1, data.getAccessions().size()));
				}
				else
				{
					secondaryAccessions.put(accession, data.getAccessions().subList(1, data.getAccessions().size()));
				}

				// for human data, we may need to update a ReferenceDNASequence.
				if (data.getScientificName().equals(HOMO_SAPIENS))
				{
					HumanDataProcessor processor = new HumanDataProcessor(adaptor, instanceEdit);
					processor.processHumanData(referenceDNASequences, referenceGeneProducts, genesOKWithENSEMBL, data, geneList, /* referenceDNASequencesForThisUniprot, */ accession);
				}
				else // Not human, but still need to process it...
				{
					NonHumanDataProcessor processor = new NonHumanDataProcessor(adaptor, instanceEdit);
					processor.processNonHumanData(referenceGeneProducts, data, accession);
				}
			}
		}
	}
}
