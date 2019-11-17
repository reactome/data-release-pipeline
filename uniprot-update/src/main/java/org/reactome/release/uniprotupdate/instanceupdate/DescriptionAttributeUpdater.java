package org.reactome.release.uniprotupdate.instanceupdate;

import org.reactome.release.uniprotupdate.dataschema.UniprotData;

import java.util.stream.Collectors;

public class DescriptionAttributeUpdater extends AttributeUpdater {
	DescriptionAttributeUpdater(String attributeToUpdate) {
		super(attributeToUpdate);
	}

	@Override
	protected String extractAttributeDataFromUniprot(UniprotData data) {
		return data.getRecommendedName() != null ?
			String.join(" ", data.getRecommendedName(), getAlternativeNames(data)) :
			"";
	}

	private String getAlternativeNames(UniprotData data) {
		return data.getAlternativeNames() != null ? String.join(" ", data.getAlternativeNames()) : "";
	}
}