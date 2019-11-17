package org.reactome.release.uniprotupdate.instanceupdate;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static org.gk.model.ReactomeJavaConstants.*;

public class AttributeUpdaterFactory {
	private static AttributeUpdaterFactory factory;
	private Map<String, Class<? extends AttributeUpdater>> attributeToUpdater;

	private AttributeUpdaterFactory() {
		registerAttributeUpdaters();
	}

	private void registerAttributeUpdaters() {
		attributeToUpdater = new HashMap<>();
		attributeToUpdater.put(secondaryIdentifier, SecondaryIdentifierAttributeUpdater.class);
		attributeToUpdater.put(description, DescriptionAttributeUpdater.class);
		attributeToUpdater.put(sequenceLength, SequenceLengthAttributeUpdater.class);
		attributeToUpdater.put(referenceGene, ReferenceGeneAttributeUpdater.class);
		attributeToUpdater.put(species, SpeciesAttributeUpdater.class);
		attributeToUpdater.put("checksum", ChecksumAttributeUpdater.class);
		attributeToUpdater.put(name, NameAttributeUpdater.class);
		attributeToUpdater.put(geneName, GeneNameAttributeUpdater.class);
		attributeToUpdater.put(comment, CommentAttributeUpdater.class);
		attributeToUpdater.put(keyword, KeywordAttributeUpdater.class);
		attributeToUpdater.put("chain", ChainAttributeUpdater.class);
	}

	public static AttributeUpdaterFactory getFactory() {
		if (factory == null) {
			factory = new AttributeUpdaterFactory();
		}

		return factory;
	}

	public AttributeUpdater createAttributeUpdater(String attribute) {
		AttributeUpdater attributeUpdater;
		try {
			Class<? extends AttributeUpdater> attributeUpdaterClass = attributeToUpdater.get(attribute);
			attributeUpdater = attributeUpdaterClass.getDeclaredConstructor(String.class).newInstance(attribute);
		} catch (NullPointerException | ReflectiveOperationException e) {
			throw new IllegalArgumentException(attribute + " is unknown and has no corresponding AttributeUpdater", e);
		}

		return attributeUpdater;
	}
}
