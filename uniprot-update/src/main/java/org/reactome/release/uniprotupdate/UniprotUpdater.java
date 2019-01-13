package org.reactome.release.uniprotupdate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.reactome.release.uniprotupdate.UniprotConstants.HOMO_SAPIENS;
import static org.reactome.release.uniprotupdate.UniprotConstants.SPECIES_TO_UPDATE;

/**
 * This class is to be used to update Uniprot instances.
 * @author sshorser
 *
 */
public class UniprotUpdater
{
	private static final Logger logger = LogManager.getLogger();

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
		AtomicInteger totalEnsemblGeneCount = new AtomicInteger(0);
		Set<String> genesOKWithENSEMBL = Collections.synchronizedSet(new HashSet<String>());
		
		String ensemblGenesFileName = "ensemblGeneIDs.list";
		// If the file already exists, load it into memory, into genesOKWithENSEMBL
		if (Files.exists(Paths.get(ensemblGenesFileName)))
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
			if (SPECIES_TO_UPDATE.contains(data.getScientificName()))
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
