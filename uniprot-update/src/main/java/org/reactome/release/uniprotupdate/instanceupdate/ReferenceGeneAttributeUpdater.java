package org.reactome.release.uniprotupdate.instanceupdate;

import org.reactome.release.uniprotupdate.dataschema.UniprotData;

public class ReferenceGeneAttributeUpdater extends AttributeUpdater {
	ReferenceGeneAttributeUpdater(String attributeToUpdate) {
		super(attributeToUpdate);
	}

	@Override
	protected Object extractAttributeDataFromUniprot(UniprotData data) {
		return null; // TODO - is there a value to return for this attribute?
	}
}