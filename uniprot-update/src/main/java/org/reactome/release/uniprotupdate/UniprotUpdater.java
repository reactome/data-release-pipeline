package org.reactome.release.uniprotupdate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

import java.util.*;
import java.util.concurrent.TimeUnit;
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
	private static final int SECONDS_UNTIL_WRITING_TO_LOG = 30;
	private long recordsProcessed = 0;

	/**
	 * Updates UniProt instances.
	 * @param uniprotData - The uniprot data that was extracted from the XML file.
	 * This will be a list of UniprotData objects, each object representing an &lt;entry/&gt; entity from the file.
	 * @param referenceDNASequences - A map of ReferenceDNASequence objects, keyed by their Identifier
	 * (ReferenceDNASequences without an identifier should not be in this list).
	 * @param referenceGeneProducts - A map of ReferenceGeneProduct objects, keyed by their Identifier
	 * (ReferenceGeneProduct without an identifier should not be in this list).
	 * @param referenceIsoforms - A map of ReferenceIsoforms objects, keyed by their VariantIdentifier
	 * (ReferenceIsoforms without an identifier should not be in this list).
	 * @throws Exception
	 */
	void updateUniprotInstances(
		MySQLAdaptor adaptor, List<UniprotData> uniprotData,
		Map<String, GKInstance> referenceDNASequences,
		Map<String, GKInstance> referenceGeneProducts,
		Map<String, GKInstance> referenceIsoforms,
		GKInstance instanceEdit
	) throws Exception
	{
		Set<String> genesOKWithENSEMBL = ENSEMBLQueryUtil.checkGenesWithENSEMBL(uniprotData, HOMO_SAPIENS);

		Map<String, List<String>> secondaryAccessions = new HashMap<>();
		long startTime = System.currentTimeMillis();
		for (UniprotData data : uniprotData)
		{
			if (secondsElapsedSince(startTime) > SECONDS_UNTIL_WRITING_TO_LOG) {
				logger.info("{} uniprot records processed ", recordsProcessed);
				startTime = System.currentTimeMillis();
			}
			// Should each pass through this loop be a single transaction?
			// This might work well if this loop is run in parallel...
			// first, let's make sure this piece of data is for a species that we can update via Uniprot Update.
			if (SPECIES_TO_UPDATE.contains(data.getScientificName()))
			{
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
					List<String> geneList = getGeneList(data);
					processor.processHumanData(referenceDNASequences, referenceGeneProducts, genesOKWithENSEMBL, data, geneList, accession);
				}
				else // Not human, but still need to process it...
				{
					NonHumanDataProcessor processor = new NonHumanDataProcessor(adaptor, instanceEdit);
					processor.processNonHumanData(referenceGeneProducts, data, accession);
				}
			}
			recordsProcessed += 1;
		}
	}

	private long secondsElapsedSince(long timePoint)
	{
		return (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - timePoint));
	}

	private List<String> getGeneList(UniprotData data)
	{
		List<String> geneList = new ArrayList<>();
		if (data.getEnsembleGeneIDs()!=null) {
			geneList = data.getEnsembleGeneIDs().stream().distinct().collect(Collectors.toList());
		}
		return geneList;
	}
}
