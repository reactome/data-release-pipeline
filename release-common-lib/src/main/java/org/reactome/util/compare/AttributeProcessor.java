package org.reactome.util.compare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;

/**
 * This class provides helper methods in processing, manipulating and obtaining attribute objects
 * for the DBObjectComparer class
 * @author jweiser
 */
class AttributeProcessor
{
	private static Map<String, Map<AttributeRelationshipType, List<SchemaAttribute>>>
		schemaClassToAttributesMap = new ConcurrentHashMap<>();

	/**
	 * Returns the attributes associated, via the passed attribute relationship type, with the passed schema class.
	 * @param schemaClass SchemaClass for which to retrieve attributes
	 * @param attributeRelationshipType Type of attributes to retrieve from the passed schema class (i.e. 'regular' or
	 * referrer attributes)
	 * @return List of SchemaAttribute objects associated, via the passed attribute relationship type, with the passed
	 * schema class
	 */
	@SuppressWarnings("unchecked")
	static List<SchemaAttribute> getAttributes(
		SchemaClass schemaClass, AttributeRelationshipType attributeRelationshipType
	)
	{
		List<SchemaAttribute> attributes =
			schemaClassToAttributesMap
				.computeIfAbsent(schemaClass.getName(), k -> new ConcurrentHashMap<>())
				.computeIfAbsent(attributeRelationshipType, k -> new ArrayList<>());

		if (attributes.isEmpty())
		{
			attributes = attributeRelationshipType.equals(AttributeRelationshipType.REGULAR_ATTRIBUTE) ?
				getDistinctSchemaAttributes(schemaClass.getAttributes()) :
				getDistinctSchemaAttributes(schemaClass.getReferers());

			schemaClassToAttributesMap
				.computeIfAbsent(schemaClass.getName(), k -> new ConcurrentHashMap<>())
				.put(attributeRelationshipType, attributes);
		}

		return attributes;
	}

	/**
	 * Filters the passed list of attributes based on the predicate function passed and returns the filtered list.
	 * @param attributes List of attributes to filter
	 * @param attributeNameFilter Predicate value used to determine which values should be included
	 * @return List of SchemaAttributes after filtering
	 */
	static List<SchemaAttribute> filterAttributes(
		List<SchemaAttribute> attributes, Predicate<? super SchemaAttribute> attributeNameFilter
	)
	{
		return attributes
			.stream()
			.filter(attributeNameFilter != null ? attributeNameFilter : getDefaultAttributeNameFilter())
			.collect(Collectors.toList());
	}

	/**
	 * Returns <code>true</code> if the attribute passed is an "instance attribute" (i.e. its value(s) is/are
	 * instance(s)); <code>false otherwise</code>.
	 * @param attribute Attribute to check for the type of value it holds
	 * @return <code>true</code> if the attribute passed holds instances; <code>false</code> otherwise
	 */
	static boolean isAttributeContainingInstances(SchemaAttribute attribute)
	{
		Class<?> attributeType = attribute.getType();

		return attributeType.equals(GKInstance.class) || attributeType.equals(Instance.class);
	}

	/**
	 * Returns the list of SchemaAttribute objects that have distinct names.
	 * @param schemaAttributes Collection of SchemaAttribute objects to filter for distinct names
	 * @return List of SchemaAttribute objects which have distinct names
	 */
	private static List<SchemaAttribute> getDistinctSchemaAttributes(Collection<SchemaAttribute> schemaAttributes)
	{
		return new ArrayList<>(schemaAttributes)
			.stream()
			.filter(distinctByKey(SchemaAttribute::getName))
			.collect(Collectors.toList());
	}

	/**
	 * Returns a predicate function that returns true if the value of type T has not been seen before, when evaluated
	 * by the passed keyExtractor function, but false otherwise.
	 *
	 * Taken from https://stackoverflow.com/questions/23699371/java-8-distinct-by-property
	 *
	 * @param keyExtractor Function to extract property from object of type T
	 * @param <T> Type of object to evaluate for distinctness via the keyExtractor
	 * @return Predicate function which accepts a function to evaluate if a specific object property has been seen
	 * before.  The predicate returns true if the object's property is distinct (i.e. not seen before) but false
	 * otherwise.
	 */
	private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor)
	{
		Set<Object> seen = ConcurrentHashMap.newKeySet();
		return t -> seen.add(keyExtractor.apply(t));
	}

	/**
	 * Returns the default attribute filter as a Predicate function.  By default, the attributes "DB_ID", "dateTime",
	 * "modified", and "created" are filtered out (i.e. ignored).
	 * @return Predicate function used to filter out SchemaAttributes value by their names
	 */
	private static Predicate<? super SchemaAttribute> getDefaultAttributeNameFilter()
	{
		/*
		 * Used for filtering out attributes that you don't want to compare because they will
		 * probably create too much noise if you do.
		 */
		return attribute ->
			!attribute.getName().equals(ReactomeJavaConstants.DB_ID) &&
			!attribute.getName().equals(ReactomeJavaConstants.dateTime) &&
			!attribute.getName().equals(ReactomeJavaConstants.modified) &&
			!attribute.getName().equals(ReactomeJavaConstants.created);
	}

	/**
	 * Represents the relationships an attribute can have with an instance or schema class:
	 *
	 * A 'regular' attribute - an attribute of an instance or schema class
	 * A referrer attribute - an attribute belonging to another instance/schema class which refers to the
	 * instance/schema class of interest
	 */
	enum AttributeRelationshipType
	{
		REGULAR_ATTRIBUTE("attribute"),
		REVERSE_ATTRIBUTE("reverse attribute");

		private final String name;

		/**
		 * Constructs an instance of the attribute relationship type enum, setting the String value
		 * to be used as the enum's name.
		 * @param name String value representing the enum
		 */
		AttributeRelationshipType(String name)
		{
			this.name = name;
		}

		/**
		 * String representation of the enum.
		 * @return The name of the enum set at the time of its creation
		 */
		public String toString()
		{
			return this.name;
		}
	}
}
