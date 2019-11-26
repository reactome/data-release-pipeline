package org.reactome.release.uniprotupdate.instanceupdate;

import org.reactome.release.uniprotupdate.dataschema.UniprotData;

import java.util.ArrayList;
import java.util.List;

public class SecondaryIdentifierAttributeUpdater extends AttributeUpdater {
	SecondaryIdentifierAttributeUpdater(String attributeToUpdate) {
		super(attributeToUpdate);
	}

	@Override
	protected List<String> extractAttributeDataFromUniprot(UniprotData data) {
		List<String> secondaryIdentifiers = new ArrayList<>();

		secondaryIdentifiers.add(data.getName());
		secondaryIdentifiers.addAll(data.getSecondaryAccessions());

		return secondaryIdentifiers;
	}
}