package org.reactome.release.uniprotupdate.instanceupdate;

import org.reactome.release.uniprotupdate.dataschema.UniprotData;

import static org.gk.model.ReactomeJavaConstants.geneName;

public class NameAttributeUpdater extends AttributeUpdater {
	NameAttributeUpdater(String attributeToUpdate) {
		super(attributeToUpdate);
	}

	@Override
	protected String extractAttributeDataFromUniprot(UniprotData data) {
		GeneNameAttributeUpdater geneNameAttributeUpdater =
			(GeneNameAttributeUpdater) AttributeUpdaterFactory.getFactory().createAttributeUpdater(geneName);

		return geneNameAttributeUpdater.extractAttributeDataFromUniprot(data).get(0);
	}
}