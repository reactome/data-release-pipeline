package org.reactome.release.uniprotupdate.instanceupdate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

public class ChecksumAttributeUpdater extends AttributeUpdater {
	private static final Logger sequencesLog = LogManager.getLogger("sequencesLog");

	ChecksumAttributeUpdater(String attributeToUpdate) {
		super(attributeToUpdate);
	}

	@Override
	protected String extractAttributeDataFromUniprot(UniprotData data) {
		return data.getSequenceChecksum() != null ? data.getSequenceChecksum() : "";
	}

	@Override
	public void updateAttribute(GKInstance instance, UniprotData data) {
		if (checksumHasChanged(instance, data)) {
			super.updateAttribute(instance, data);
			sequencesLog.info(
				"Checksum has changed! DB ID: {} \tOld checksum: {} \tNew checksum: {}",
				instance.getDBID(), getOldChecksum(instance), extractAttributeDataFromUniprot(data)
			);
		}
		updateIsSequenceChangedAttribute(instance, checksumHasChanged(instance, data));
	}

	private void updateIsSequenceChangedAttribute(GKInstance instance, boolean checksumHasChanged) {
		try {
			instance.setAttributeValue("isSequenceChanged", checksumHasChanged);
			instance.getDbAdaptor().updateInstanceAttribute(instance, "isSequenceChanged");
		} catch (Exception e) {
			logger.error("Could not update the isSequenceChanged attribute for instance {}",
				instance.getExtendedDisplayName(), e);
		}
	}

	private boolean checksumHasChanged(GKInstance instance, UniprotData data) {
		String newChecksum = extractAttributeDataFromUniprot(data);

		return newChecksum != null && !newChecksum.isEmpty() && !newChecksum.equals(getOldChecksum(instance));
	}

	private String getOldChecksum(GKInstance instance) {
		try {
			return (String) instance.getAttributeValue(getAttributeToUpdate());
		} catch (Exception e) {
			logger.error("Could not get old checksum value from instance {}", instance.getExtendedDisplayName(),e);
			return "";
		}
	}
}