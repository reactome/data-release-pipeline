package org.reactome.release.common.database;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

/**
 * This class provides methods to create InstanceEdits as GKInstance objects
 * @author sshorser
 */
public class InstanceEditUtils
{
	/**
	 * Creates an InstanceEdit object associated with a specific person id and creator name
	 *
	 * @param dba MySQLAdaptor connecting to the database for which the InstanceEdit will be created and stored
	 * @param personID ID of the associated Person instance in the database
	 * @param creatorName The name of the thing that is creating this InstanceEdit.  Typically, you would want to use
	 * the package and classname that uses <i>this</i> object, so it can be traced to the appropriate part of the
	 * program.
	 * @return InstanceEdit as a GKInstance object
	 * @throws Exception Thrown if unable to create the GKInstance object representing an InstanceEdit
	 */
	public static GKInstance createInstanceEdit(MySQLAdaptor dba, long personID, String creatorName)
		throws Exception
	{
		GKInstance instanceEdit = createDefaultIE(dba, personID, true, "Inserted by " + creatorName);
		instanceEdit.getDBID();
		dba.updateInstance(instanceEdit);
		return instanceEdit;
	}

	// This code below was taken from 'add-links' repo:
	// org.reactomeaddlinks.db.ReferenceCreator
	/**
	 * Creates and saves in the database a default InstanceEdit associated with the
	 * Person entity whose DB_ID is <i>defaultPersonId</i>.
	 *
	 * @param dba MySQLAdaptor connecting to the database for which the InstanceEdit will be created and stored
	 * @param defaultPersonId ID of the associated Person instance in the database
	 * @param needStore true if the created InstanceEdit should be stored in the database; false otherwise
	 * @return InstanceEdit as a GKInstance object.
	 * @throws Exception Thrown if unable to retrieve the Person instance associated with the defaultPersonId
	 */
	public static GKInstance createDefaultIE(MySQLAdaptor dba, long defaultPersonId, boolean needStore, String note)
		throws Exception
	{
		GKInstance defaultPerson = dba.fetchInstance(defaultPersonId);
		if (defaultPerson != null)
		{
			GKInstance newIE = createDefaultInstanceEdit(defaultPerson);
			newIE.addAttributeValue(ReactomeJavaConstants.dateTime, GKApplicationUtilities.getDateTime());
			newIE.addAttributeValue(ReactomeJavaConstants.note, note);
			InstanceDisplayNameGenerator.setDisplayName(newIE);

			if (needStore)
			{
				dba.storeInstance(newIE);
			}
			return newIE;
		}
		else
		{
			throw new Exception("Could not fetch Person entity with ID " + defaultPersonId + ". Please check that a " +
				"Person entity exists in the database with this ID.");
		}
	}

	/**
	 * Creates an InstanceEdit for with a Person instance as its author
	 *
	 * @param person Person instance to associate as the InstanceEdit author
	 * @return InstanceEdit as GKInstance object
	 * @throws InvalidAttributeValueException Thrown if the person argument is an invalid value for the "author"
	 * attribute of the InstanceEdit
	 */
	public static GKInstance createDefaultInstanceEdit(GKInstance person)
		throws InvalidAttributeValueException
	{
		GKInstance instanceEdit = new GKInstance();
		PersistenceAdaptor adaptor = person.getDbAdaptor();
		instanceEdit.setDbAdaptor(adaptor);
		SchemaClass cls = adaptor.getSchema().getClassByName(ReactomeJavaConstants.InstanceEdit);
		instanceEdit.setSchemaClass(cls);

		try
		{
			instanceEdit.addAttributeValue(ReactomeJavaConstants.author, person);
		}
		catch (InvalidAttributeException e)
		{
			throw new RuntimeException("Fatal error: The 'author' attribute was invalid for the GKInstance object "
				+ "created for the new InstanceEdit", e);
		}

		return instanceEdit;
	}
}
