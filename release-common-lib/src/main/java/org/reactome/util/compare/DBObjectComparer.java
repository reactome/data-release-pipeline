package org.reactome.util.compare;

import static org.reactome.util.general.CollectionUtils.combineLists;
import static org.reactome.util.general.CollectionUtils.safeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaAttribute;

/**
 * This class can be used to perform comparisons on any two DatabaseObjects across two different databases.
 * (It assumes that they have the same DB_ID).
 * @author sshorser
 */
public class DBObjectComparer
{
	private static Map<GKInstance, Map<SchemaAttribute, List<Object>>> instanceAttributeToValuesMap = new HashMap<>();
	private static Map<GKInstance, List<SchemaAttribute>> instanceToRegularAttributesMap = new HashMap<>();
	private static Map<GKInstance, List<SchemaAttribute>> instanceToReferrerAttributesMap = new HashMap<>();

	/**
	 * Compares two GKInstances.
	 * @param instance1 the first instance.
	 * @param instance2 the second instance.
	 * @param stringBuilder a StringBuilder that will contain a detailed report of differences.
	 * @return The number of differences between the two instances.
	 * A single-valued attribute that differs will count as 1 diff.
	 * If the instances have different schema classes, that will count as 1 diff.
	 * If a multi-valued attribute has a different number of elements between the two instances,
	 * that will count as 1 diff and the elements will NOT be compared.
	 */
	public static int compareInstances(GKInstance instance1, GKInstance instance2, StringBuilder stringBuilder)
	{
		final int maxRecusionDepth = 5;
		final boolean checkReferrers = false;

		return compareInstances(instance1, instance2, stringBuilder, maxRecusionDepth, checkReferrers);
	}

	/**
	 * Compares two GKInstances.
	 * @param instance1 the first instance.
	 * @param instance2 the second instance.
	 * @param stringBuilder a StringBuilder that will contain a detailed report of differences.
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
		final int maxRecusionDepth = 5;

		return compareInstances(instance1, instance2, stringBuilder, maxRecusionDepth, checkReferrers);
	}

	/**
	 * Compares two GKInstances.
	 * @param instance1 the first instance.
	 * @param instance2 the second instance.
	 * @param stringBuilder a StringBuilder that will contain a detailed report of differences.
	 * @param maxRecursionDepth the maximum depth of recursion that will be allowed. Normally a depth of 2 or 3 is
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
		final int diffCount = 0;
		final int initalRecursionDepth = 0;
		final Predicate<? super SchemaAttribute> customAttributeNameFilter = null;

		return compareInstances(
			instance1, instance2, stringBuilder, diffCount, initalRecursionDepth,
			maxRecursionDepth, customAttributeNameFilter, checkReferrers
		);
	}

	/**
	 * Compares two GKInstances.
	 * @param instance1 the first instance.
	 * @param instance2 the second instance.
	 * @param stringBuilder a StringBuilder that will contain a detailed report of differences.
	 * @param maxRecursionDepth the maximum depth of recursion that will be allowed. Normally a depth of 2 or 3 is
	 * probably sufficient.
	 * @param customAttributeNameFilter A custom Predicate that will be used to filter attribute names. The default
	 * predicate looks like this:<pre>
Predicate&lt;? super SchemaAttribute&gt; attributeNameFilter = a -&gt; {
						return !a.getName().equals("DB_ID")
							&& !a.getName().equals("dateTime")
							&& !a.getName().equals("modified")
							&& !a.getName().equals("created");
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
		final int diffCount = 0;
		final int initalRecursionDepth = 0;

		return compareInstances(
			instance1, instance2, stringBuilder, diffCount, initalRecursionDepth,
			maxRecursionDepth, customAttributeNameFilter, checkReferrers
		);
	}

	/**
	 * Recursively compares two GKInstances.
	 * @param instance1 the first instance.
	 * @param instance2 the second instance.
	 * @param stringBuilder a StringBuilder that will contain a detailed report of differences.
	 * @param diffCount The number of differences so far. Should start at 0.
	 * @param recursionDepth The depth of the recursion so far. Should start at 0.
	 * @param customAttributeNameFilter A custom Predicate that will be used to filter attribute names.
	 * The default predicate looks like this:<pre>
Predicate&lt;? super SchemaAttribute&gt; attributeNameFilter = a -&gt; {
						return !a.getName().equals("DB_ID")
							&& !a.getName().equals("dateTime")
							&& !a.getName().equals("modified")
							&& !a.getName().equals("created");
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
		for (SchemaAttribute attribute : getAllAttributes(instance1, checkReferrers, customAttributeNameFilter))
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

					count = compareInstanceValues(
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

	private static boolean isInstanceEdit(GKInstance instance)
	{
		return instance.getSchemClass().getName().equals("InstanceEdit");
	}

	private static boolean differentInstanceTypes(GKInstance instance1, GKInstance instance2)
	{
		String instanceType1 = instance1.getSchemClass().getName();
		String instanceType2 = instance2.getSchemClass().getName();

		return !instanceType1.equals(instanceType2);
	}

	private static String getIndentString(int recursionDepth)
	{
		String[] indentArray = new String[recursionDepth * 2];
		Arrays.fill(indentArray, " ");
		String indentString = String.join("", indentArray);

		return indentString;
	}

	private static String getInstanceTypeMismatchMessage(GKInstance instance1, GKInstance instance2) {
		return "Schema classes don't match, so instances can't be compared! " +
			"Instance 1 is a " + instance1.getSchemClass().getName() + " and " +
			"Instance 2 is a " + instance2.getSchemClass().getName() +
			System.lineSeparator();
	}

	private static String getCountMismatchMessage(
		GKInstance instance1, GKInstance instance2, SchemaAttribute attribute
	)
	{
		return "Count mismatch for multi-valued" + getAttributeRelationshipType(instance1, attribute) +
			" attribute '" + attribute.getName() + "' Instance 1 ('" + instance1 +
			"') has " + getValues(instance1, attribute).size() + " elements but Instance 2 ('" + instance2 +
			"') has " + getValues(instance2, attribute).size() + " elements." + System.lineSeparator();
	}

	private static List<SchemaAttribute> getAllAttributes(
		GKInstance instance, boolean checkReferrers, Predicate<? super SchemaAttribute> attributeNameFilter
	)
	{
		List<SchemaAttribute> allAttributes = checkReferrers ?
			combineLists(getRegularAttributes(instance), getReferrerAttributes(instance)) :
			getRegularAttributes(instance);

		return filterAttributes(allAttributes, attributeNameFilter);
	}

	@SuppressWarnings("unchecked")
	private static List<SchemaAttribute> getRegularAttributes(GKInstance instance)
	{
		List<SchemaAttribute> regularAttributes = instanceToRegularAttributesMap.get(instance);
		if (regularAttributes == null)
		{
			regularAttributes = new ArrayList<SchemaAttribute>(instance.getSchemClass().getAttributes());
			instanceToRegularAttributesMap.put(instance, regularAttributes);
		}
		return regularAttributes;
	}

	@SuppressWarnings("unchecked")
	private static List<SchemaAttribute> getReferrerAttributes(GKInstance instance)
	{
		List<SchemaAttribute> referrerAttributes = instanceToReferrerAttributesMap.get(instance);
		if (referrerAttributes == null)
		{
			referrerAttributes = new ArrayList<SchemaAttribute>(instance.getSchemClass().getReferers());
			instanceToReferrerAttributesMap.put(instance, referrerAttributes);
		}
		return referrerAttributes;
	}

	private static List<SchemaAttribute> filterAttributes(
		List<SchemaAttribute> attributes, Predicate<? super SchemaAttribute> attributeNameFilter
	)
	{
		return attributes
			.stream()
			.filter(attributeNameFilter != null ? attributeNameFilter : getDefaultAttributeNameFilter())
			.collect(Collectors.toList());
	}

	private static Predicate<? super SchemaAttribute> getDefaultAttributeNameFilter()
	{
		/*
		 * Used for filtering out attributes that you don't want to compare because they will
		 * probably create too much noise if you do.
		 */
		return a -> {
			return !a.getName().equals(ReactomeJavaConstants.DB_ID)
				&& !a.getName().equals(ReactomeJavaConstants.dateTime)
				&& !a.getName().equals(ReactomeJavaConstants.modified)
				&& !a.getName().equals(ReactomeJavaConstants.created);
		};
	}

	private static List<Object> getValues(GKInstance instance, SchemaAttribute attribute)
	{
		List<Object> values = getCachedValuesFromInstanceAttributeToValuesMap(instance, attribute);

		if (values.isEmpty())
		{
			values = getRegularAttributes(instance).contains(attribute) ?
				constructGetValuesFunctionForRegularAttributes(instance).apply(attribute) :
				constructGetValuesFunctionForReferrerAttributes(instance).apply(attribute);

		// Make sure the lists are sorted so that you are always comparing objects in the same
		// sequence: I don't think the database adaptor applies any explicit order to Instances
		// that don't have a rank/order attribute.
		InstanceUtilities.sortInstances(values);

			setCachedValuesFromInstanceAttributeToValuesMap(instance, attribute, values);
		}
		return values;
	}

	private static List<Object> getCachedValuesFromInstanceAttributeToValuesMap(
		GKInstance instance, SchemaAttribute attribute
	)
	{
		return instanceAttributeToValuesMap
			.computeIfAbsent(instance, instanceKey -> new HashMap<>())
			.computeIfAbsent(attribute, attributeKey -> new ArrayList<>());
	}

	private static void setCachedValuesFromInstanceAttributeToValuesMap(
		GKInstance instance, SchemaAttribute attribute, List<Object> values
	)
	{
		instanceAttributeToValuesMap
			.computeIfAbsent(instance, instanceKey -> new HashMap<>())
			.put(attribute, values);
	}

	private static Function<SchemaAttribute, List<Object>> constructGetValuesFunctionForRegularAttributes(
		GKInstance instance
	)
	{
		return (attribute) ->
		{
			try
			{
				@SuppressWarnings("unchecked")
				List<Object> values = safeList((Collection<Object>) instance.getAttributeValuesList(attribute));
				return values;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return new ArrayList<>();
			}
		};
	}

	private static Function<SchemaAttribute, List<Object>> constructGetValuesFunctionForReferrerAttributes(
		GKInstance instance
	)
	{
		return (attribute) ->
		{
			try
			{
				@SuppressWarnings("unchecked")
				List<Object> referrerValues = safeList((Collection<Object>) instance.getReferers(attribute));
				return referrerValues;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return new ArrayList<>();
			}
		};
	}

	private static int compareInstanceValues(
		SchemaAttribute attribute, Object value1, Object value2, GKInstance instance1, GKInstance instance2,
		StringBuilder sb, int diffCount, int recursionDepth, int maxRecursionDepth,
		Predicate<? super SchemaAttribute> customAttributeNameFilter, boolean checkReferrers
	) {
		int count = diffCount;
		// Deal with attributes that return GKInstance objects.
		if (isInstanceAttribute(attribute))
		{
			if (recursionDepth < maxRecursionDepth)
			{
				sb.append(getIndentString(recursionDepth))
					.append(" Recursing on ")
					.append(attribute.getName())
					.append(" attribute...")
					.append(System.lineSeparator());

				return compareInstances(
					(GKInstance) value1, (GKInstance) value2, sb, count, recursionDepth + 1,
					maxRecursionDepth, customAttributeNameFilter, checkReferrers
				);
			}
		}
		// Deal with attributes that return "simple" things (Strings, numbers, etc..., arrays of
		// Strings/numbers/etc...)
		else if (!value1.equals(value2))
		{
			sb.append(
				getIndentString(recursionDepth) + "Mismatch on" +
					getAttributeRelationshipType(instance1, attribute) + " attribute " +
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

	private static boolean isInstanceAttribute(SchemaAttribute attribute)
	{
		Class<?> attributeType = attribute.getType();

		return attributeType.equals(GKInstance.class) || attributeType.equals(Instance.class);
	}

	private static String getAttributeRelationshipType(GKInstance instance, SchemaAttribute attribute)
	{
		return getReferrerAttributes(instance).contains(attribute) ? "referrer attribute" : "attribute";
	}
}
