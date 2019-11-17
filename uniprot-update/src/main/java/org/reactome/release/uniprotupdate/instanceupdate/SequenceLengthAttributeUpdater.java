package org.reactome.release.uniprotupdate.instanceupdate;

import org.reactome.release.uniprotupdate.dataschema.UniprotData;

public class SequenceLengthAttributeUpdater extends AttributeUpdater {
	private static final Integer NO_SEQUENCE_LENGTH_VALUE = -1;

	SequenceLengthAttributeUpdater(String attributeToUpdate) {
		super(attributeToUpdate);
	}

	@Override
	protected Integer extractAttributeDataFromUniprot(UniprotData data) {
		return data.getSequenceLength() != null ? Integer.valueOf(data.getSequenceLength()) : NO_SEQUENCE_LENGTH_VALUE;
	}
}