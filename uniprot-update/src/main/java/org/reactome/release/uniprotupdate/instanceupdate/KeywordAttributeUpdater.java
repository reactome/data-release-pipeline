package org.reactome.release.uniprotupdate.instanceupdate;

import org.reactome.release.uniprotupdate.dataschema.UniprotData;

import java.util.ArrayList;
import java.util.List;

public class KeywordAttributeUpdater extends AttributeUpdater {
	KeywordAttributeUpdater(String attributeToUpdate) {
		super(attributeToUpdate);
	}

	@Override
	protected List<String> extractAttributeDataFromUniprot(UniprotData data) {
		return !data.getFlattenedKeywords().isEmpty() ? data.getFlattenedKeywords() : new ArrayList<>();
	}
}
