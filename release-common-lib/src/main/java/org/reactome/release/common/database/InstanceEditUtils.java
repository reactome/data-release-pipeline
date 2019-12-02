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
		GKInstance instanceEdit = null;
		try
		{
			instanceEdit = createDefaultIE(dba, personID, true, "Inserted by " + creatorName);
			instanceEdit.getDBID();
			dba.updateInstance(instanceEdit);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw e;
		}
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
	public static GKInstance createDefaultIE(MySQLAdaptor dba, Long defaultPersonId, boolean needStore, String note)
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
	 */
	public static GKInstance createDefaultInstanceEdit(GKInstance person)
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
		catch (InvalidAttributeException | InvalidAttributeValueException e)
		{
			e.printStackTrace();
			// throw this back up the stack - no way to recover from in here.
			throw new Error(e);
		}

		return instanceEdit;
	}
}
