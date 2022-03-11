package org.reactome.release.chebiupdate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.reactome.util.compare.DBObjectComparer;

public class JavaVsPerlComparison
{
	private static final int MAX_COMPARISON_RECURSION_DEPTH = 1;
	private static final boolean CHECK_REFERRERS = true;

	public static void main(String[] args) throws Exception
	{
		MySQLAdaptor javaUpdatedDB = new MySQLAdaptor("localhost", "perl_gk_central", "root", "root", 3306);
		MySQLAdaptor perlUpdatedDB = new MySQLAdaptor("localhost", "java_gk_central", "root", "root", 3306);
		int diffCount = 0;
		int sameCount = 0;

		@SuppressWarnings("unchecked")
		String chebiRefDBID = (new ArrayList<GKInstance>(
			javaUpdatedDB.fetchInstanceByAttribute(
				ReactomeJavaConstants.ReferenceDatabase, ReactomeJavaConstants.name, "=", "ChEBI")
			)
		).get(0).getDBID().toString();

		@SuppressWarnings("unchecked")
		Collection<GKInstance> javaRefMolecules = (Collection<GKInstance>) javaUpdatedDB.fetchInstanceByAttribute(
			ReactomeJavaConstants.ReferenceMolecule, ReactomeJavaConstants.referenceDatabase, "=", chebiRefDBID
		);

		for (GKInstance javaInstance : javaRefMolecules)
		{
			StringBuilder sb = new StringBuilder();
			GKInstance perlInstance = perlUpdatedDB.fetchInstance(javaInstance.getDBID());

			// This is a filter, to filter instances out of the comparison, based on attributes.
			//
			// This filter excludes anything where the attribute is one of: name, displayName,
			// referenceEntity, formula, identifier AND the object in question is a SimpleEntity and the attribute
			// is referenceEntity.
			Predicate<? super SchemaAttribute> customAttributeNameFilter = schemaAttribute -> {
				// Is the attribute's name in the list?
				boolean nameIsInList = (new HashSet<>(
					Arrays.asList(
						ReactomeJavaConstants.name, ReactomeJavaConstants._displayName,
						ReactomeJavaConstants.referenceEntity, ReactomeJavaConstants.formula,
						ReactomeJavaConstants.identifier
					)
				)).contains(schemaAttribute.getName());

				// This condition below is to avoid recursing from the SimpleEntity back the ReferenceMolecule via
				// SimpleEntity's referenceEntity attribute.  The maxRecursionDepth would prevent things from getting
				// too out of hand, but you'll still get a lot of redundant diffs if you don't do this.
				// Is the attribute "referenceEntity" and is it on a SimpleEntity object?
				@SuppressWarnings("unchecked")
				boolean simpleEntityWithReferenceEntityAttribute =
					(new ArrayList<SchemaClass>(
						schemaAttribute.getSchemaClass()
					)).get(0).getName().equals(ReactomeJavaConstants.SimpleEntity) &&
					schemaAttribute.getName().equals(ReactomeJavaConstants.referenceEntity);

				return nameIsInList && !simpleEntityWithReferenceEntityAttribute;
			};

			int currentDiff = DBObjectComparer.compareInstances(
				javaInstance, perlInstance, sb, MAX_COMPARISON_RECURSION_DEPTH, customAttributeNameFilter,
				CHECK_REFERRERS
			);

			if (currentDiff > 0)
			{
				System.out.println("===\nDiffs on instance: " + javaInstance.toString());
				System.out.println(sb.toString());
			}
			else
			{
				sameCount++;
			}

			diffCount += currentDiff;
		}

		System.out.println(
			"Number of ReferenceMolecule diffs: " + diffCount + "; " +
			"Number of sames: " + sameCount + "; " +
			"Total number of ReferenceMolecules compared: " + javaRefMolecules.size()
		);
	}
}
