package org.reactome.util.compare;

import static org.reactome.util.general.CollectionUtils.combineLists;
import static org.reactome.util.general.CollectionUtils.safeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;

/**
 * This class can be used to perform comparisons on any two DatabaseObjects across two different databases.
 * (It assumes that they have the same DB_ID).
 * @author sshorser
 */
public class DBObjectComparer
{
	private static Map<GKInstance, Map<SchemaAttribute, List<Object>>> instanceAttributeToValuesMap = new HashMap<>();
	private static Map<String, List<SchemaAttribute>> schemaClassToRegularAttributesMap = new HashMap<>();
	private static Map<String, List<SchemaAttribute>> schemaClassToReferrerAttributesMap = new HashMap<>();

	private static final int DEFAULT_MAX_RECURSION_DEPTH = 5;
	private static final int DEFAULT_INSTANCES_DIFFERENCES_COUNT = 0;
	private static final int DEFAULT_RECURSION_DEPTH = 0;

	/**
	 * Compares two GKInstances.
	 * @param instance1 The first instance.
	 * @param instance2 The second instance.
	 * @param stringBuilder A StringBuilder that will contain a detailed report of differences.
	 * @return The number of differences between the two instances.
	 * A single-valued attribute that differs will count as 1 diff.
	 * If the instances have different schema classes, that will count as 1 diff.
	 * If a multi-valued attribute has a different number of elements between the two instances,
	 * that will count as 1 diff and the elements will NOT be compared.
	 */
	public static int compareInstances(GKInstance instance1, GKInstance instance2, StringBuilder stringBuilder)
	{
		final boolean checkReferrers = false;

		return compareInstances(instance1, instance2, stringBuilder, DEFAULT_MAX_RECURSION_DEPTH, checkReferrers);
	}

	/**
	 * Compares two GKInstances.
	 * @param instance1 The first instance.
	 * @param instance2 The second instance.
	 * @param stringBuilder A StringBuilder that will contain a detailed report of differences.
	 * @param checkReferrers Should referring instances also be checked? If <b>true</b>, then referring attributes
	 * will <em>also</em> be checked for differences. They will be followed to the same recursion depth as regular
	 * attributes. Using this with a high maxRecursionDepth could lead to a very long execution time. Be careful!!
	 * You might also want to use a custom predicate to filter out attributes that could lead to a cyclical difference
	 * check.
	 * @return The number of differences between the two instances.
	 * A single-valued attribute that differs will count as 1 diff.
	 * If the instances have different schema classes, that will count as 1 diff.
	 * If a multi-valued attribute has a different number of elements between the two instances,
	 * that will count as 1 diff and the elements will NOT be compared.
	 */
	public static int compareInstances(
		GKInstance instance1, GKInstance instance2, StringBuilder stringBuilder, boolean checkReferrers
	)
	{
		return compareInstances(instance1, instance2, stringBuilder, DEFAULT_MAX_RECURSION_DEPTH, checkReferrers);
	}

	/**
	 * Compares two GKInstances.
	 * @param instance1 The first instance.
	 * @param instance2 The second instance.
	 * @param stringBuilder A StringBuilder that will contain a detailed report of differences.
	 * @param maxRecursionDepth The maximum depth of recursion that will be allowed. Normally a depth of 2 or 3 is
	 * probably sufficient.
	 * @param checkReferrers Should referring instances also be checked? If <b>true</b>, then referring attributes
	 * will <em>also</em> be checked for differences.  They will be followed to the same recursion depth as regular
	 * attributes. Using this with a high maxRecursionDepth could lead to a very long execution time. Be careful!!
	 * You might also want to use a custom predicate to filter out attributes that could lead to a cyclical difference
	 * check.
	 * @return The number of differences between the two instances.
	 * A single-valued attribute that differs will count as 1 diff.
	 * If the instances have different schema classes, that will count as 1 diff.
	 * If a multi-valued attribute has a different number of elements between the two instances,
	 * that will count as 1 diff and the elements will NOT be compared.
	 */
	public static int compareInstances(
		GKInstance instance1, GKInstance instance2, StringBuilder stringBuilder,
		int maxRecursionDepth, boolean checkReferrers
	)
	{
		// Given named constant for context in using this value as an argument
		final Predicate<? super SchemaAttribute> customAttributeNameFilter = null;

		return compareInstances(
			instance1, instance2, stringBuilder, DEFAULT_INSTANCES_DIFFERENCES_COUNT,
			DEFAULT_RECURSION_DEPTH, maxRecursionDepth, customAttributeNameFilter, checkReferrers
		);
	}

	/**
	 * Compares two GKInstances.
	 * @param instance1 The first instance.
	 * @param instance2 The second instance.
	 * @param stringBuilder A StringBuilder that will contain a detailed report of differences.
	 * @param maxRecursionDepth The maximum depth of recursion that will be allowed. Normally a depth of 2 or 3 is
	 * probably sufficient.
	 * @param customAttributeNameFilter A custom Predicate that will be used to filter attribute names. The default
	 * predicate looks like this:<pre>
Predicate&lt;? super SchemaAttribute&gt; attributeNameFilter = a -&gt; {
						return !a.getName().equals("DB_ID")
							&amp;&amp; !a.getName().equals("dateTime")
							&amp;&amp; !a.getName().equals("modified")
							&amp;&amp; !a.getName().equals("created");
					};
	 </pre>
	 * @param checkReferrers Should referring instances also be checked? If <b>true</b>, then referring attributes
	 * will <em>also</em> be checked for differences.  They will be followed to the same recursion depth as regular
	 * attributes. Using this with a high maxRecursionDepth could lead to a very long execution time. Be careful!!
	 * You might also want to use a custom predicate to filter out attributes that could lead to a cyclical difference
	 * check.
	 * @return The number of differences between the two instances.
	 * A single-valued attribute that differs will count as 1 diff.
	 * If the instances have different schema classes, that will count as 1 diff.
	 * If a multi-valued attribute has a different number of elements between the two instances,
	 * that will count as 1 diff and the elements will NOT be compared.
	 */
	public static int compareInstances(
		GKInstance instance1, GKInstance instance2, StringBuilder stringBuilder, int maxRecursionDepth,
		Predicate<? super SchemaAttribute> customAttributeNameFilter, boolean checkReferrers
	)
	{
		return compareInstances(
			instance1, instance2, stringBuilder, DEFAULT_INSTANCES_DIFFERENCES_COUNT,
			DEFAULT_RECURSION_DEPTH, maxRecursionDepth, customAttributeNameFilter, checkReferrers
		);
	}

	/**
	 * Recursively compares two GKInstances.
	 * @param instance1 The first instance.
	 * @param instance2 The second instance.
	 * @param stringBuilder A StringBuilder that will contain a detailed report of differences.
	 * @param diffCount The number of differences so far. Should start at 0.
	 * @param recursionDepth The depth of the recursion so far. Should start at 0.
	 * @param customAttributeNameFilter A custom Predicate that will be used to filter attribute names.
	 * The default predicate looks like this:<pre>
Predicate&lt;? super SchemaAttribute&gt; attributeNameFilter = a -&gt; {
						return !a.getName().equals("DB_ID")
							&amp;&amp; !a.getName().equals("dateTime")
							&amp;&amp; !a.getName().equals("modified")
							&amp;&amp; !a.getName().equals("created");
					};
	 </pre>
	 * @param checkReferrers Should referring instances also be checked? If <b>true</b>, then referring attributes
	 * will <em>also</em> be checked for differences.  They will be followed to the same recursion depth as regular
	 * attributes. Using this with a high maxRecursionDepth could lead to a very long execution time. Be careful!!
	 * You might also want to use a custom predicate to filter out attributes that could lead to a cyclical difference
	 * check.
	 * @return The number of differences between the two instances.
	 * A single-valued attribute that differs will count as 1 diff.
	 * If the instances have different schema classes, that will count as 1 diff.
	 * If a multi-valued attribute has a different number of elements between the two instances,
	 * that will count as 1 diff and the elements will NOT be compared.
	 */
	private static int compareInstances(
		GKInstance instance1, GKInstance instance2, StringBuilder stringBuilder, int diffCount, int recursionDepth,
		int maxRecursionDepth, Predicate<? super SchemaAttribute> customAttributeNameFilter, boolean checkReferrers
	)
	{
		if (instance1 == null || instance2 == null || isInstanceEdit(instance1) || isInstanceEdit(instance2))
		{
			return diffCount;
		}

		if (differentInstanceTypes(instance1, instance2))
		{
			stringBuilder
			.append(getIndentString(recursionDepth))
			.append(getInstanceTypeMismatchMessage(instance1, instance2));

			return 1;
		}

		int count = diffCount;
		List<SchemaAttribute> allAttributes = getAllAttributes(
			instance1.getSchemClass(), checkReferrers, customAttributeNameFilter
		);
		for (SchemaAttribute attribute : allAttributes)
		{
			List<Object> instance1AttributeValues = getValues(instance1, attribute);
			List<Object> instance2AttributeValues = getValues(instance2, attribute);

			if (instance1AttributeValues.size() == instance2AttributeValues.size())
			{
				// compare each item in one list to the corresponding item in the other list -
				// the MySQLAdaptor seems to preserve sequence of items in lists properly.
				for (int i = 0; i < instance1AttributeValues.size(); i++)
				{
					Object value1 = instance1AttributeValues.get(i);
					Object value2 = instance2AttributeValues.get(i);

					count = compareValuesOfAttributeBetweenInstances(
						attribute, value1, value2, instance1, instance2, stringBuilder, count, recursionDepth,
						maxRecursionDepth, customAttributeNameFilter, checkReferrers
					);
				}
			}
			else
			{
				stringBuilder.append(getIndentString(recursionDepth))
					.append(getCountMismatchMessage(instance1, instance2, attribute));

				count++;
			}
		}
		return count;
	}

	/**
	 * Checks passed GKInstance object to determine if it is an InstanceEdit instance or not.  Returns
	 * <code>true</code> if the instance is an InstanceEdit; <code>false</code> otherwise
	 * @param instance GKInstance object to check
	 * @return <code>true</code> if instance is an InstanceEdit; <code>false</code> otherwise
	 */
	private static boolean isInstanceEdit(GKInstance instance)
	{
		return instance.getSchemClass().getName().equals("InstanceEdit");
	}

	/**
	 * Compares two instances and determines if their schema class types are different.  Returns true if the instances
	 * are of different types; false otherwise
	 * @param instance1 First instance to compare
	 * @param instance2 Second instance to compare
	 * @return <code>true</code> if the instances are of different types; <code>false</code> otherwise
	 */
	private static boolean differentInstanceTypes(GKInstance instance1, GKInstance instance2)
	{
		String instanceType1 = instance1.getSchemClass().getName();
		String instanceType2 = instance2.getSchemClass().getName();

		return !instanceType1.equals(instanceType2);
	}

	/**
	 * Returns a String of empty spaces that is twice the length of the recursion depth passed.  This provides an
	 * indentation for visualizing log statements at different levels of recursion in checking instances in the
	 * compareInstances methods.
	 * @param recursionDepth Level of recursion correlating to the indentation returned
	 * @return String of empty spaces twice the length of the recursionDepth
	 */
	private static String getIndentString(int recursionDepth)
	{
		String[] indentArray = new String[recursionDepth * 2];
		Arrays.fill(indentArray, " ");
		String indentString = String.join("", indentArray);

		return indentString;
	}

	/**
	 * Returns a message detailing a mismatch in schema class type between two instances.  The schema class of each
	 * instance passed will be mentioned in the message.
	 * @param instance1 First instance in the mismatch
	 * @param instance2 Second instance in the mismatch
	 * @return Message as String detailing the schema class mismatch between the two instances.
	 */
	private static String getInstanceTypeMismatchMessage(GKInstance instance1, GKInstance instance2)
	{
		return "Schema classes don't match, so instances can't be compared! " +
			"Instance 1 is a " + instance1.getSchemClass().getName() + " and " +
			"Instance 2 is a " + instance2.getSchemClass().getName() +
			System.lineSeparator();
	}

	/**
	 * Returns a message detailing a mismatch in the number of values of a given attribute when comparing two
	 * instances.  The instances, attribute name, and number of values obtained from each instance will be mentioned in
	 * the message.
	 * @param instance1 First instance in the mismatch
	 * @param instance2 Second instance in the mismatch
	 * @param attribute Attribute from which values were obtained
	 * @return Message as String detailing the mismatch in the number of values for the passed attribute between the
	 * passed instances
	 */
	private static String getCountMismatchMessage(
		GKInstance instance1, GKInstance instance2, SchemaAttribute attribute
	)
	{
		return "Count mismatch for " + getAttributeRelationshipType(instance1.getSchemClass(), attribute) +
			" '" + attribute.getName() + "'" + System.lineSeparator() +
			" Instance 1 ('" + instance1 + "' from " + getDBName(instance1) + ") has " +
			getValues(instance1, attribute).size() + " elements " + System.lineSeparator() +
			" Instance 2 ('" + instance2 + "' from " + getDBName(instance2) + ") has " +
			getValues(instance2, attribute).size() + " elements" + System.lineSeparator() + System.lineSeparator();
	}

	private static String getDBName(GKInstance instance)
	{
		return ((MySQLAdaptor) instance.getDbAdaptor()).getDBName();
	}

	/**
	 * Retrieves all schema attributes for the passed schema class with options to include referrer attributes and to
	 * filter results.  Uses a cache for the attributes (before filtering) of schema classes which have been previously
	 * queried.
	 * @param schemaClass Schema class for which to get attributes
	 * @param checkReferrers <code>true</code> if referrer attributes should be included; <code>false</code> otherwise
	 * @param attributeNameFilter Predicate value used to determine which attributes should be included
	 * @return List of SchemaAttributes of the schema class passed
	 */
	private static List<SchemaAttribute> getAllAttributes(
		SchemaClass schemaClass, boolean checkReferrers, Predicate<? super SchemaAttribute> attributeNameFilter
	)
	{
		List<SchemaAttribute> allAttributes = checkReferrers ?
			combineLists(getRegularAttributes(schemaClass), getReferrerAttributes(schemaClass)) :
			getRegularAttributes(schemaClass);

		return filterAttributes(allAttributes, attributeNameFilter);
	}

	/**
	 * Retrieves "regular" (i.e. not referrer) attributes for the passed schema class.  Uses a cache for the
	 * attributes of schema classes which have been previously queried.
	 * @param schemaClass Schema class for which to get attributes
	 * @return List of "regular" (i.e. not referrer) SchemaAttributes of the schema class object passed
	 */
	@SuppressWarnings("unchecked")
	private static List<SchemaAttribute> getRegularAttributes(SchemaClass schemaClass)
	{
		List<SchemaAttribute> regularAttributes = schemaClassToRegularAttributesMap.get(schemaClass.getName());
		if (regularAttributes == null)
		{
			regularAttributes = new ArrayList<SchemaAttribute>(schemaClass.getAttributes());
			schemaClassToRegularAttributesMap.put(schemaClass.getName(), regularAttributes);
		}
		return regularAttributes;
	}

	/**
	 * Retrieves referrer attributes for the passed schema class (i.e. attributes used by instances to refer to the
	 * passed schema class).  Uses a cache for the attributes of schema classes which have been previously
	 * queried.
	 * @param schemaClass Schema class for which to get referrer attributes
	 * @return List of referrer SchemaAttributes of the schema class object passed
	 */
	@SuppressWarnings("unchecked")
	private static List<SchemaAttribute> getReferrerAttributes(SchemaClass schemaClass)
	{
		List<SchemaAttribute> referrerAttributes = schemaClassToReferrerAttributesMap.get(schemaClass.getName());
		if (referrerAttributes == null)
		{
			referrerAttributes = new ArrayList<SchemaAttribute>(schemaClass.getReferers());
			schemaClassToReferrerAttributesMap.put(schemaClass.getName(), referrerAttributes);
		}
		return referrerAttributes;
	}

	/**
	 * Filters the passed list of attributes based on the predicate function passed and returns the filtered list.
	 * @param attributes List of attributes to filter
	 * @param attributeNameFilter Predicate value used to determine which values should be included
	 * @return List of SchemaAttributes after filtering
	 */
	private static List<SchemaAttribute> filterAttributes(
		List<SchemaAttribute> attributes, Predicate<? super SchemaAttribute> attributeNameFilter
	)
	{
		return attributes
			.stream()
			.filter(attributeNameFilter != null ? attributeNameFilter : getDefaultAttributeNameFilter())
			.collect(Collectors.toList());
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
		return attribute -> {
			return !attribute.getName().equals(ReactomeJavaConstants.DB_ID)
				&& !attribute.getName().equals(ReactomeJavaConstants.dateTime)
				&& !attribute.getName().equals(ReactomeJavaConstants.modified)
				&& !attribute.getName().equals(ReactomeJavaConstants.created);
		};
	}

	/**
	 * Retrieves the values for the passed attribute of the passed instance.  Uses a cache for the values of attributes
	 * (cached by the combination of instance and attribute) which have been previously queried.
	 * @param instance Instance to query for the values of one of its attributes
	 * @param attribute Attribute to query for its values from the instance
	 * @return List of generic Objects containing the values of the specific instance and attribute
	 */
	private static List<Object> getValues(GKInstance instance, SchemaAttribute attribute)
	{
		List<Object> values = getCachedValuesFromInstanceAttributeToValuesMap(instance, attribute);

		if (values.isEmpty())
		{
			values = getRegularAttributes(instance.getSchemClass()).contains(attribute) ?
				getValuesFunctionForRegularAttributes(instance, attribute) :
				getValuesFunctionForReferrerAttributes(instance, attribute);

			// Make sure the lists are sorted so that you are always comparing objects in the same
			// sequence: I don't think the database adaptor applies any explicit order to Instances
			// that don't have a rank/order attribute.
			if (isAttributeContainingInstances(attribute))
			{
				InstanceUtilities.sortInstances(values);
			}

			setCachedValuesFromInstanceAttributeToValuesMap(instance, attribute, values);
		}
		return values;
	}

	/**
	 * Retrieves cached values for a specific attribute in a specific instance.  An empty list is returned if nothing
	 * has yet been cached.
	 * @param instance Instance to query for the cached values of one of its attributes
	 * @param attribute Attribute to query for its cached values in the instance
	 * @return List of generic Objects containing the values of the specific instance and attribute that are cached.
	 * An empty list is returned if nothing has yet been cached.
	 */
	private static List<Object> getCachedValuesFromInstanceAttributeToValuesMap(
		GKInstance instance, SchemaAttribute attribute
	)
	{
		return instanceAttributeToValuesMap
			.computeIfAbsent(instance, instanceKey -> new HashMap<>())
			.computeIfAbsent(attribute, attributeKey -> new ArrayList<>());
	}

	/**
	 * Caches the values passed, associating them to the specific instance and attribute passed
	 * @param instance Instance associated with the values being cached
	 * @param attribute Attribute associated with the values being cached
	 * @param values Values to cache
	 */
	private static void setCachedValuesFromInstanceAttributeToValuesMap(
		GKInstance instance, SchemaAttribute attribute, List<Object> values
	)
	{
		instanceAttributeToValuesMap
			.computeIfAbsent(instance, instanceKey -> new HashMap<>())
			.put(attribute, values);
	}

	/**
	 * Returns the values on the passed instance for the "regular" (i.e. not referrer) attribute
	 * passed to the method.
	 * @param instance Instance from which to retrieve values on the passed attribute
	 * @param attribute Attribute from which to retrieve values for the passed instance
	 * @return List of values for the instance and attribute passed or an empty list if there is an issue retrieving
	 * the values
	 */
	private static List<Object> getValuesFunctionForRegularAttributes(
		GKInstance instance, SchemaAttribute attribute
	)
	{
		try
		{
			@SuppressWarnings("unchecked")
			List<Object> values = safeList((Collection<Object>) instance.getAttributeValuesList(attribute.getName()));
			return values;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	/**
	 * Returns the values connected to the passed instance for the referrer attribute (i.e. attributes used
	 * by other instances to refer to the passed instance) passed to the method.
	 * @param instance Instance for which to retrieve values connected via the passed referrer attribute
	 * @param attribute Referrer attribute for which to retrieve values connected to the passed instance
	 * @return List of values for the instance and referrer attribute passed or an empty list if there is an issue
	 * retrieving the values
	 */
	private static List<Object> getValuesFunctionForReferrerAttributes(
		GKInstance instance, SchemaAttribute attribute
	)
	{
		try
		{
			@SuppressWarnings("unchecked")
			List<Object> referrerValues = safeList((Collection<Object>) instance.getReferers(attribute.getName()));
			return referrerValues;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	/**
	 * Compares the value of the passed attribute between two instances and returns the number of differences.  For an
	 * value which is a GKInstance, differences are checked for recursively (up to the passed maxRecursionDepth).
	 * For the base case of "simple" value (i.e. Strings, numbers, etc..., arrays of Strings/numbers/etc...), a count
	 * of 1 is returned for any difference found between the values compared.
	 * @param attribute Attribute for which values are being compared
	 * @param value1 First value to compare
	 * @param value2 Second value to compare
	 * @param instance1 First instance from which value was obtained
	 * @param instance2 Second instance from which value was obtained
	 * @param stringBuilder a StringBuilder that will contain a detailed report of differences.
	 * @param diffCount The number of differences so far. Should start at 0.
	 * @param recursionDepth The depth of the recursion so far. Should start at 0.
	 * @param maxRecursionDepth The maximum depth of recursion that will be allowed. Normally a depth of 2 or 3 is
	 * probably sufficient.
	 * @param customAttributeNameFilter A custom Predicate that will be used to filter attribute names. The default
	 * predicate looks like this:<pre>
	Predicate&lt;? super SchemaAttribute&gt; attributeNameFilter = a -&gt; {
	return !a.getName().equals("DB_ID")
	&amp;&amp; !a.getName().equals("dateTime")
	&amp;&amp; !a.getName().equals("modified")
	&amp;&amp; !a.getName().equals("created");
	};
	</pre>
	 * @param checkReferrers <code>true</code> if referrer attributes should be included; <code>false</code> otherwise
	 * @return The total number of differences between the values compared (after recursion for GKInstance values)
	 */
	private static int compareValuesOfAttributeBetweenInstances(
		SchemaAttribute attribute, Object value1, Object value2, GKInstance instance1, GKInstance instance2,
		StringBuilder stringBuilder, int diffCount, int recursionDepth, int maxRecursionDepth,
		Predicate<? super SchemaAttribute> customAttributeNameFilter, boolean checkReferrers
	) {
		int count = diffCount;
		// Deal with attributes that return GKInstance objects.
		if (isAttributeContainingInstances(attribute))
		{
			if (recursionDepth < maxRecursionDepth)
			{
				stringBuilder.append(getIndentString(recursionDepth))
					.append(" Recursing on ")
					.append(attribute.getName())
					.append(" attribute...")
					.append(System.lineSeparator());

				return compareInstances(
					(GKInstance) value1, (GKInstance) value2, stringBuilder, count, recursionDepth + 1,
					maxRecursionDepth, customAttributeNameFilter, checkReferrers
				);
			}
		}
		// Deal with attributes that return "simple" things (Strings, numbers, etc..., arrays of
		// Strings/numbers/etc...)
		else if (!value1.equals(value2))
		{
			stringBuilder.append(
				getIndentString(recursionDepth) + "Mismatch on" +
					getAttributeRelationshipType(instance1.getSchemClass(), attribute) + " attribute " +
					"'" + attribute.getName() + "'" + System.lineSeparator() +
					getIndentString(recursionDepth) + "Instance 1 ('" + instance1 + "') has value:\t" +
					value1 + System.lineSeparator() +
					getIndentString(recursionDepth) + "Instance 2 ('" + instance2 + "') has value:\t" +
					value2 + System.lineSeparator()
			);
			count++;
		}
		return count;
	}

	/**
	 * Returns <code>true</code> if the attribute passed is an "instance attribute" (i.e. its value(s) is/are
	 * instance(s)); <code>false otherwise</code>
	 * @param attribute Attribute to check for the type of value it holds
	 * @return <code>true</code> if the attribute passed holds instances; <code>false</code> otherwise
	 */
	private static boolean isAttributeContainingInstances(SchemaAttribute attribute)
	{
		Class<?> attributeType = attribute.getType();

		return attributeType.equals(GKInstance.class) || attributeType.equals(Instance.class);
	}

	/**
	 * Returns a String describing the type of the relationship the passed attribute has to the passed schema class.
	 *
	 * The String will be "referrer attribute" if the attribute passed is a referrer attribute for the schema class
	 * passed.  Otherwise, the String will be "attribute" to indicate it is a 'regular' attribute.
	 * @param schemaClass SchemaClass to check to determine the relationship of the attribute passed
	 * @param attribute Attribute to check for the relationship to the schema class passed
	 * @return String describing the attribute's relationship to the schema class. "referrer attribute" for a referrer
	 * attribute or "attribute" for a 'regular' attribute.
	 */
	private static String getAttributeRelationshipType(SchemaClass schemaClass, SchemaAttribute attribute)
	{
		return getReferrerAttributes(schemaClass).contains(attribute) ? "referrer attribute" : "attribute";
	}
}
