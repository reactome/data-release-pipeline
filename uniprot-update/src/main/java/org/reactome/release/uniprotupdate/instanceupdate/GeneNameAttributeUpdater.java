package org.reactome.release.uniprotupdate.instanceupdate;

import org.reactome.release.uniprotupdate.dataschema.UniprotData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GeneNameAttributeUpdater extends AttributeUpdater {
	GeneNameAttributeUpdater(String attributeToUpdate) {
		super(attributeToUpdate);
	}

	@Override
	protected List<String> extractAttributeDataFromUniprot(UniprotData data) {
		if (data.getFlattenedGeneNames() != null && data.getFlattenedGeneNames().size() > 0) {
			return data.getFlattenedGeneNames().stream().distinct().collect(Collectors.toList());
		} else {
			return new ArrayList<>();
		}
	}
}