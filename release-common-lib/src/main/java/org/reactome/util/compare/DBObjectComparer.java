package org.reactome.util.compare;

import static org.reactome.util.compare.AttributeProcessor.isAttributeContainingInstances;
import static org.reactome.util.general.CollectionUtils.safeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaAttribute;
import org.reactome.util.compare.AttributeProcessor.AttributeRelationshipType;

/**
 * This class can be used to perform comparisons on any two DatabaseObjects across two different databases.
 * (It assumes that they have the same DB_ID).
 * @author sshorser
 * @author jweiser
 */
public class DBObjectComparer
{
	private static Map<GKInstance, Map<SchemaAttribute, Map<AttributeRelationshipType, List<Object>>>>
		instanceAttributeToValuesMap = new ConcurrentHashMap<>();

	private static final int DEFAULT_MAX_RECURSION_DEPTH = 5;
	private static final int DEFAULT_INSTANCES_DIFFERENCES_COUNT = 0;
	private static final int DEFAULT_RECURSION_DEPTH = 0;

	/**
	 * Compares two GKInstances.
	 * @param instance1 The first instance
	 * @param instance2 The second instance
	 * @param stringBuilder A StringBuilder that will contain a detailed report of differences
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
	 * @param instance1 The first instance
	 * @param instance2 The second instance
	 * @param stringBuilder A StringBuilder that will contain a detailed report of differences
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
	 * @param instance1 The first instance
	 * @param instance2 The second instance
	 * @param stringBuilder A StringBuilder that will contain a detailed report of differences
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
	 * @param instance1 The first instance
	 * @param instance2 The second instance
	 * @param stringBuilder A StringBuilder that will contain a detailed report of differences
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
	 * @param instance1 The first instance
	 * @param instance2 The second instance
	 * @param stringBuilder A StringBuilder that will contain a detailed report of differences
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

		int count = compareValuesOfInstancesForAttributes(
			AttributeRelationshipType.REGULAR_ATTRIBUTE, instance1, instance2, stringBuilder, diffCount,
			recursionDepth, maxRecursionDepth, customAttributeNameFilter, checkReferrers
		);

		if (checkReferrers) {
			count = compareValuesOfInstancesForAttributes(
				AttributeRelationshipType.REVERSE_ATTRIBUTE, instance1, instance2, stringBuilder, count,
				recursionDepth, maxRecursionDepth, customAttributeNameFilter, checkReferrers
			);
		}

		return count;
	}

	/**
	 * Recursively compares two GKInstances.
	 * @param attributeRelationshipType Relationship between the passed attribute to the passed instances (i.e. a
	 * 'regular' or referrer attribute)
	 * @param instance1 The first instance
	 * @param instance2 The second instance
	 * @param stringBuilder A StringBuilder that will contain a detailed report of differences
	 * @param diffCount The number of differences so far. Should start at 0.
	 * @param recursionDepth The depth of the recursion so far. Should start at 0.
	 * @param maxRecursionDepth The maximum depth of recursion that will be allowed. Normally a depth of 2 or 3 is
	 * probably sufficient.
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
	private static int compareValuesOfInstancesForAttributes(
		AttributeRelationshipType attributeRelationshipType, GKInstance instance1, GKInstance instance2,
		StringBuilder stringBuilder, int diffCount, int recursionDepth, int maxRecursionDepth,
		Predicate<? super SchemaAttribute> customAttributeNameFilter, boolean checkReferrers
	)
	{
		List<SchemaAttribute> attributes = AttributeProcessor.filterAttributes(
			AttributeProcessor.getAttributes(instance1.getSchemClass(), attributeRelationshipType),
			customAttributeNameFilter
		);

		int count = diffCount;
		for (SchemaAttribute attribute : attributes)
		{
			List<Object> instance1AttributeValues = getValues(
				instance1, attribute, attributeRelationshipType
			);
			List<Object> instance2AttributeValues = getValues(
				instance2, attribute, attributeRelationshipType
			);

			if (instance1AttributeValues.size() == instance2AttributeValues.size())
			{
				count = compareEachValueOfAttributeBetweenInstances(
					attribute, attributeRelationshipType, instance1, instance2, instance1AttributeValues,
					instance2AttributeValues, stringBuilder, count, recursionDepth, maxRecursionDepth,
					customAttributeNameFilter, checkReferrers
				);
			}
			else
			{
				stringBuilder.append(getIndentString(recursionDepth))
					.append(getCountMismatchMessage(
						attribute, attributeRelationshipType, instance1, instance2,
						instance1AttributeValues.size(), instance2AttributeValues.size()
					));

				count++;
			}
		}

		return count;
	}

	/**
	 * Compares the values of the passed attribute between two instances and returns the number of differences.  For an
	 * value which is a GKInstance, differences are checked for recursively (up to the passed maxRecursionDepth).
	 * For the base case of "simple" value (i.e. Strings, numbers, etc..., arrays of Strings/numbers/etc...), a count
	 * of 1 is returned for any difference found between the values compared.
	 * @param attribute Attribute for which values are being compared
	 * @param attributeRelationshipType Relationship between the passed attribute to the passed instances (i.e. a
	 * 'regular' or referrer attribute)
	 * @param instance1 The first instance
	 * @param instance2 The second instance
	 * @param values1 Values retrieved for the passed attribute from the first instance
	 * @param values2 Values retrieved for the passed attribute from the second instance
	 * @param stringBuilder A StringBuilder that will contain a detailed report of differences
	 * @param diffCount The number of differences so far. Should start at 0.
	 * @param recursionDepth The depth of the recursion so far. Should start at 0.
	 * @param maxRecursionDepth The maximum depth of recursion that will be allowed. Normally a depth of 2 or 3 is
	 * probably sufficient.
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
	private static int compareEachValueOfAttributeBetweenInstances(
		SchemaAttribute attribute, AttributeRelationshipType attributeRelationshipType, GKInstance instance1,
		GKInstance instance2, List<Object> values1, List<Object> values2, StringBuilder stringBuilder, int diffCount,
		int recursionDepth, int maxRecursionDepth, Predicate<? super SchemaAttribute> customAttributeNameFilter,
		boolean checkReferrers
	)
	{
		int count = diffCount;

		// compare each item in one list to the corresponding item in the other list -
		// the MySQLAdaptor seems to preserve sequence of items in lists properly.
		for (int i = 0; i < values1.size(); i++)
		{
			Object value1 = values1.get(i);
			Object value2 = values2.get(i);

			count = compareIndividualValuesOfAttributeBetweenInstances(
				attribute, attributeRelationshipType, value1, value2, instance1, instance2, stringBuilder, count,
				recursionDepth, maxRecursionDepth, customAttributeNameFilter, checkReferrers
			);
		}

		return count;
	}

	/**
	 * Compares the value of the passed attribute between two instances and returns the number of differences.  For an
	 * value which is a GKInstance, differences are checked for recursively (up to the passed maxRecursionDepth).
	 * For the base case of "simple" value (i.e. Strings, numbers, etc..., arrays of Strings/numbers/etc...), a count
	 * of 1 is returned for any difference found between the values compared.
	 * @param attribute Attribute for which values are being compared
	 * @param attributeRelationshipType Relationship between the attribute and the instances passed (i.e. 'regular' or
	 * referrer attribute
	 * @param value1 First value to compare
	 * @param value2 Second value to compare
	 * @param instance1 Instance from which the first value was obtained
	 * @param instance2 Instance from which the second value was obtained
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
	private static int compareIndividualValuesOfAttributeBetweenInstances(
		SchemaAttribute attribute, AttributeRelationshipType attributeRelationshipType, Object value1, Object value2,
		GKInstance instance1, GKInstance instance2, StringBuilder stringBuilder, int diffCount, int recursionDepth,
		int maxRecursionDepth, Predicate<? super SchemaAttribute> customAttributeNameFilter, boolean checkReferrers
	) {
		int count = diffCount;
		// Deal with attributes that return GKInstance objects.
		if (isAttributeContainingInstances(attribute))
		{
			if (recursionDepth < maxRecursionDepth)
			{
				String recursionMessage =
					"Recursing on " + attribute.getName() + " " + attributeRelationshipType + "..." +
					System.lineSeparator();

				stringBuilder
					.append(getIndentString(recursionDepth))
					.append(recursionMessage);

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
				getIndentString(recursionDepth) + "Mismatch on " + attributeRelationshipType +
					" '" + attribute.getName() + "'" + System.lineSeparator() +
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
	 * Checks passed GKInstance object to determine if it is an InstanceEdit instance or not.  Returns
	 * <code>true</code> if the instance is an InstanceEdit; <code>false</code> otherwise
	 * @param instance GKInstance object to check
	 * @return <code>true</code> if instance is an InstanceEdit; <code>false</code> otherwise
	 */
	private static boolean isInstanceEdit(GKInstance instance)
	{
		return instance.getSchemClass().getName().equals(ReactomeJavaConstants.InstanceEdit);
	}

	/**
	 * Compares two instances and determines if their schema class types are different.  Returns <code>true</code> if
	 * the instances are of different types; <code>false</code> otherwise.
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
	 * @param attribute Attribute from which values were obtained
	 * @param attributeRelationship Relationship between the passed attribute to the passed instances (i.e. a
	 * 'regular' or referrer attribute)
	 * @param instance1 First instance in the mismatch
	 * @param instance2 Second instance in the mismatch
	 * @param values1Size Number of values obtained from the first instance
	 * @param values2Size Number of values obtained from the second instance
	 * @return Message as String detailing the mismatch in the number of values for the passed attribute between the
	 * passed instances
	 */
	private static String getCountMismatchMessage(
		SchemaAttribute attribute, AttributeRelationshipType attributeRelationship, GKInstance instance1,
		GKInstance instance2, int values1Size, int values2Size
	)
	{
		return "Count mismatch for " + attributeRelationship +
			" '" + attribute.getName() + "'" + System.lineSeparator() +
			" Instance 1 ('" + instance1 + "' from " + getDBName(instance1) + ") has " +
			values1Size + " elements " + System.lineSeparator() +
			" Instance 2 ('" + instance2 + "' from " + getDBName(instance2) + ") has " +
			values2Size + " elements" + System.lineSeparator() + System.lineSeparator();
	}

	/**
	 * Returns the name of the database from which the passed instance originates.
	 * @param instance Instance for which to get the name of the database from which it originated
	 * @return Name of the database of origin for the passed instance
	 */
	private static String getDBName(GKInstance instance)
	{
		return ((MySQLAdaptor) instance.getDbAdaptor()).getDBName();
	}


	/**
	 * Retrieves cached values for a specific attribute and attributeRelationshipType in a specific instance.  An empty
	 * list is returned if nothing has yet been cached.
	 * @param instance Instance to query for the cached values of one of its attributes
	 * @param attribute Attribute to query for its cached values in the instance
	 * @param attributeRelationshipType Relationship of the passed attribute to the passed instance (i.e. a
	 * 'regular' or referrer attribute)
	 * @return List of generic Objects containing the values of the specific instance, attribute, and
	 * attributeRelationshipType that are cached.  An empty list is returned if nothing has yet been cached.
	 */
	private static List<Object> getCachedValuesFromInstanceAttributeToValuesMap(
		GKInstance instance, SchemaAttribute attribute, AttributeRelationshipType attributeRelationshipType
	)
	{
		return instanceAttributeToValuesMap
			.computeIfAbsent(instance, instanceKey -> new ConcurrentHashMap<>())
			.computeIfAbsent(attribute, attributeKey -> new ConcurrentHashMap<>())
			.computeIfAbsent(attributeRelationshipType, attributeRelationshipTypeKey -> new ArrayList<>());
	}

	/**
	 * Caches the values passed, associating them to the specific instance, attribute, and attributeRelationshipType
	 * passed.
	 * @param instance Instance associated with the values being cached
	 * @param attribute Attribute associated with the values being cached
	 * @param attributeRelationshipType Relationship of the passed attribute to the passed instance (i.e. a
	 * 'regular' or referrer attribute)
	 * @param values Values to cache
	 */
	private static void setCachedValuesFromInstanceAttributeToValuesMap(
		GKInstance instance, SchemaAttribute attribute, AttributeRelationshipType attributeRelationshipType,
		List<Object> values
	)
	{
		instanceAttributeToValuesMap
			.computeIfAbsent(instance, instanceKey -> new ConcurrentHashMap<>())
			.computeIfAbsent(attribute, attributeKey -> new ConcurrentHashMap<>())
			.put(attributeRelationshipType, values);
	}


	/**
	 * Returns the values connected to the passed instance for the attribute passed to the method via the attribute
	 * relationship type (i.e. 'regular' or referrer attribute).
	 * @param instance Instance for which to retrieve values connected via the passed attribute and attribute
	 * relationship type
	 * @param attribute Attribute for which to retrieve values connected to the passed instance
	 * @param attributeRelationshipType Relationship between the passed attribute to the passed instance (i.e. a
	 * 'regular' or referrer attribute)
	 * @return List of values for the instance attribute and attribute relationship type passed or an empty list if
	 * there is an issue retrieving the values
	 */
	@SuppressWarnings("unchecked")
	private static List<Object> getValues(
		GKInstance instance, SchemaAttribute attribute, AttributeRelationshipType attributeRelationshipType
	)
	{
		List<Object> values = getCachedValuesFromInstanceAttributeToValuesMap(
			instance, attribute, attributeRelationshipType
		);

		if (values.isEmpty())
		{
			try
			{
				values = attributeRelationshipType.equals(AttributeRelationshipType.REGULAR_ATTRIBUTE) ?
					safeList((List<Object>) instance.getAttributeValuesList(attribute.getName())) :
					safeList((Collection<Object>) instance.getReferers(attribute.getName()));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			// Make sure the lists are sorted so that you are always comparing objects in the same
			// sequence: I don't think the database adaptor applies any explicit order to Instances
			// that don't have a rank/order attribute.
			if (isAttributeContainingInstances(attribute))
			{
				InstanceUtilities.sortInstances(values);
			}

			setCachedValuesFromInstanceAttributeToValuesMap(instance, attribute, attributeRelationshipType, values);
		}

		return values;
	}
}
