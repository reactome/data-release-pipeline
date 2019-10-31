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

public class InstanceEditUtils {

	/**
	 * Create an InstanceEdit.
	 *
	 * @param personID ID of the associated Person entity.
	 * @param creatorName The name of the person/thing that is creating this InstanceEdit.
	 * Typically, you would want to use the package and classname that
	 * uses <i>this</i> object, so it can be traced to the appropriate
	 * part of the program.
	 * @return An InstanceEdit object
	 * @throws Exception Thrown if unable to create the instance edit
	 */
	public static GKInstance createInstanceEdit(MySQLAdaptor adaptor, long personID, String creatorName)
		throws Exception {
		GKInstance instanceEdit = createDefaultIE(adaptor, personID, true, "Inserted by " + creatorName);
		adaptor.updateInstance(instanceEdit);

		return instanceEdit;
	}

	// This code below was taken from 'add-links' repo:
	// org.reactomeaddlinks.db.ReferenceCreator
	/**
	 * Create and save in the database a default InstanceEdit associated with the
	 * Person entity whose DB_ID is <i>defaultPersonId</i>.
	 *
	 * @param dba Database adaptor from which to fetch the person instance and store the created instance edit
	 * @param defaultPersonId Database identifier for the person instance to fetch
	 * @param needStore True if the created instance edit should be stored, false otherwise
	 * @return An InstanceEdit object.
	 * @throws Exception Thrown if unable to fetch person instance or create/store instance edit for the
	 * fetch person instance
	 */
	public static GKInstance createDefaultIE(MySQLAdaptor dba, Long defaultPersonId, boolean needStore, String note)
		throws Exception {
		GKInstance defaultPerson = dba.fetchInstance(defaultPersonId);
		if (defaultPerson == null) {
			throw new Exception(
				"Could not fetch Person entity with ID " + defaultPersonId + ". " +
					"Please check that a Person entity exists in the database with this ID."
			);
		}

		GKInstance newIE = createDefaultInstanceEdit(defaultPerson);
		newIE.addAttributeValue(ReactomeJavaConstants.dateTime, GKApplicationUtilities.getDateTime());
		newIE.addAttributeValue(ReactomeJavaConstants.note, note);
		InstanceDisplayNameGenerator.setDisplayName(newIE);

		if (needStore) {
			dba.storeInstance(newIE);
		}
		return newIE;
	}

	/**
	 * Create an InstanceEdit object setting the author to the Person object passed
	 *
	 * @param person Person object to use as the author of the instance edit
	 * @return An InstanceEdit object
	 * @throws InvalidAttributeException Thrown if the attribute "author" used for the instance edit is invalid
	 * @throws InvalidAttributeValueException Thrown if the value for person is invalid for the instance edit
	 * author attribute
	 */
	public static GKInstance createDefaultInstanceEdit(GKInstance person)
		throws InvalidAttributeException, InvalidAttributeValueException {
		PersistenceAdaptor adaptor = person.getDbAdaptor();

		GKInstance instanceEdit = new GKInstance();
		instanceEdit.setDbAdaptor(adaptor);
		instanceEdit.setSchemaClass(getInstanceEditSchemaClass(adaptor));
		instanceEdit.addAttributeValue(ReactomeJavaConstants.author, person);

		return instanceEdit;
	}

	private static SchemaClass getInstanceEditSchemaClass(PersistenceAdaptor adaptor) {
		return adaptor.getSchema().getClassByName(ReactomeJavaConstants.InstanceEdit);
	}
}
