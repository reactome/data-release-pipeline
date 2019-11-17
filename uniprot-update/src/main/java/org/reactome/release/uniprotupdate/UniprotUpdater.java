package org.reactome.release.uniprotupdate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

import java.util.*;

import static org.reactome.release.uniprotupdate.UniprotConstants.HOMO_SAPIENS;
import static org.reactome.release.uniprotupdate.UniprotConstants.SPECIES_TO_UPDATE;

/**
 * This class is to be used to update Uniprot instances.
 * @author sshorser
 *
 */
public class UniprotUpdater {
	private static final Logger logger = LogManager.getLogger();

	private MySQLAdaptor adaptor;
	private List<UniprotData> includedUniProtEntries;
	private List<UniprotData> excludedUniProtEntries;

	public UniprotUpdater(MySQLAdaptor adaptor, List<UniprotData> uniProtEntries) {
		this.adaptor = adaptor;
		setUniProtEntries(uniProtEntries);
	}

	private void setUniProtEntries(List<UniprotData> uniProtEntries) {
		this.includedUniProtEntries = new ArrayList<>();
		this.excludedUniProtEntries = new ArrayList<>();

		for (UniprotData uniprotEntry : uniProtEntries) {
			if (isUniProtEntryOfSpeciesToUpdate(uniprotEntry)) {
				this.includedUniProtEntries.add(uniprotEntry);
			} else {
				this.excludedUniProtEntries.add(uniprotEntry);
			}
		}
	}

	private boolean isUniProtEntryOfSpeciesToUpdate(UniprotData uniprotEntry) {
		return SPECIES_TO_UPDATE.contains(uniprotEntry.getScientificName());
	}

	public MySQLAdaptor getAdaptor() {
		return this.adaptor;
	}

	public List<UniprotData> getIncludedUniprotEntries() {
		return this.includedUniProtEntries;
	}

	public List<UniprotData> getExcludedUniProtEntries() { return this.excludedUniProtEntries; }

	/**
	 * Updates UniProt instances.
	 * This will be a list of UniprotData objects, each object representing an &lt;entry/&gt; entity from the file.
	 * @throws Exception
	 */
	void updateUniprotInstances(GKInstance instanceEdit) throws Exception {
		//Set<String> genesOKWithENSEMBL = ENSEMBLQueryUtil.checkGenesWithENSEMBL(getUniprotEntries(), HOMO_SAPIENS);

		int recordsProcessed = 0;
		for (UniprotData uniprotEntry : getIncludedUniprotEntries()) {
			updateUniProtInstance(uniprotEntry, instanceEdit);

			if (recordsProcessed++ % 100 == 0) {
				logger.info("{} uniprot records processed ", recordsProcessed);
			}
		}
	}

	private void updateUniProtInstance(UniprotData uniprotEntry, GKInstance instanceEdit) throws Exception {
		// for human data, we may need to update a ReferenceDNASequence.
		if (uniprotEntry.getScientificName().equals(HOMO_SAPIENS)) {
			HumanDataProcessor processor = new HumanDataProcessor(getAdaptor(), instanceEdit, getIncludedUniprotEntries());
			processor.processData(uniprotEntry);
		} else { // Not human, but still need to process it...
			NonHumanDataProcessor processor = new NonHumanDataProcessor(getAdaptor(), instanceEdit);
			processor.processData(uniprotEntry);
		}
	}
}
