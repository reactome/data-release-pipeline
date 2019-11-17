package org.reactome.release.uniprotupdate.instanceupdate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AttributeUpdater {
	static final Logger logger = LogManager.getLogger();
	protected String attributeToUpdate;

	protected abstract Object extractAttributeDataFromUniprot(UniprotData data);

	AttributeUpdater(String attributeToUpdate) {
		this.attributeToUpdate = attributeToUpdate;
	}

	public void updateAttribute(GKInstance instance, UniprotData data) {
		try {
			instance.setAttributeValue(getAttributeToUpdate(), extractAttributeDataFromUniprot(data));
			instance.getDbAdaptor().updateInstanceAttribute(instance, getAttributeToUpdate());
		} catch (Exception e) {
			logger.error(
				"Unable to update attribute {} for instance {}",
				getAttributeToUpdate(),
				instance.getExtendedDisplayName(),
				e
			);
		}
	}

	String getAttributeToUpdate() {
		return this.attributeToUpdate;
	}
}
