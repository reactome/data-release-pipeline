package org.reactome.release.uniprotupdate;

import java.util.concurrent.atomic.AtomicInteger;
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

	/**
	 * Updates UniProt instances.
	 * @param uniprotEntries - The uniprot data that was extracted from the XML file.
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
		MySQLAdaptor adaptor,
		List<UniprotData> uniprotEntries,
		Map<String, GKInstance> referenceDNASequences,
		Map<String, GKInstance> referenceGeneProducts,
		Map<String, GKInstance> referenceIsoforms,
		GKInstance instanceEdit
	) throws Exception
	{
		Set<String> genesOKWithENSEMBL = ENSEMBLQueryUtil.checkGenesWithENSEMBL(uniprotEntries, HOMO_SAPIENS);

		AtomicInteger recordsProcessed = new AtomicInteger();

		uniprotEntries.stream()
			.filter(uniprotEntry -> SPECIES_TO_UPDATE.contains(uniprotEntry.getScientificName()))
			.forEach(uniprotEntry -> {

				updateUniProtInstance(
					adaptor,
					uniprotEntry,
					genesOKWithENSEMBL,
					referenceDNASequences,
					referenceGeneProducts,
					referenceIsoforms,
					instanceEdit
				);

				if (recordsProcessed.getAndIncrement() % 100 == 0) {
					logger.info("{} uniprot records processed ", recordsProcessed);
				}
			});
	}

	private void updateUniProtInstance(
		MySQLAdaptor adaptor,
		UniprotData uniprotEntry,
		Set<String> genesOKWithENSEMBL,
		Map<String, GKInstance> referenceDNASequences,
		Map<String, GKInstance> referenceGeneProducts,
		Map<String, GKInstance> referenceIsoforms,
		GKInstance instanceEdit
	) {
		// for human data, we may need to update a ReferenceDNASequence.
		if (uniprotEntry.getScientificName().equals(HOMO_SAPIENS)) {
			HumanDataProcessor processor = new HumanDataProcessor(adaptor, instanceEdit);
			processor.processHumanData(
				referenceDNASequences,
				referenceGeneProducts,
				genesOKWithENSEMBL,
				uniprotEntry
			);
		} else { // Not human, but still need to process it...
			NonHumanDataProcessor processor = new NonHumanDataProcessor(adaptor, instanceEdit);
			processor.processNonHumanData(referenceGeneProducts, uniprotEntry);
		}
	}


}
