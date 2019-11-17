package org.reactome.release.uniprotupdate.instanceupdate;

import org.reactome.release.uniprotupdate.dataschema.UniprotData;

public class CommentAttributeUpdater extends AttributeUpdater {
	CommentAttributeUpdater(String attributeToUpdate) {
		super(attributeToUpdate);
	}

	@Override
	protected String extractAttributeDataFromUniprot(UniprotData data) {
		return !data.getFlattenedCommentsText().isEmpty() ? data.getFlattenedCommentsText() : "";
	}
}