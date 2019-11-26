package org.reactome.release.uniprotupdate.instanceupdate;

import org.reactome.release.uniprotupdate.dataschema.UniprotData;

public class DescriptionAttributeUpdater extends AttributeUpdater {
	DescriptionAttributeUpdater(String attributeToUpdate) {
		super(attributeToUpdate);
	}

	@Override
	protected String extractAttributeDataFromUniprot(UniprotData data) {
		StringBuilder description = new StringBuilder();

		if (data.getRecommendedName() != null) {
			description.append(data.getRecommendedName().toString());
			description.append(" ");
		}

		description.append(getAlternativeNames(data));

		return description.toString().trim();
	}

	private String getAlternativeNames(UniprotData data) {
		return String.join(" ", data.getAlternativeNamesAsStrings());
	}
}