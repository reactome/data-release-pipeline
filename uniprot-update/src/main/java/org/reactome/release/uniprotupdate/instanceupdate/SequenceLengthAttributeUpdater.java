package org.reactome.release.uniprotupdate.instanceupdate;

import org.reactome.release.uniprotupdate.dataschema.UniprotData;

public class SequenceLengthAttributeUpdater extends AttributeUpdater {
	SequenceLengthAttributeUpdater(String attributeToUpdate) {
		super(attributeToUpdate);
	}

	@Override
	protected Integer extractAttributeDataFromUniprot(UniprotData data) {
		return data.getSequenceLength();
	}
}