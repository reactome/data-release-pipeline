package org.reactome.release;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaClass;

import java.util.Arrays;

public class BioModelsUtilities {

    private static final Logger logger = LogManager.getLogger();

    public static GKInstance fetchBiomodelsReferenceDatabase(MySQLAdaptor dba, GKInstance instanceEdit) {
        //TODO Need to make this retrieval case insensitive using a REGEX
        GKInstance biomodelsReferenceDatabase = null;
        logger.info("Attempting to fetch an existing BioModels reference database");
        try {
            biomodelsReferenceDatabase = (GKInstance) dba.fetchInstanceByAttribute(
                    ReactomeJavaConstants.ReferenceDatabase,
                    ReactomeJavaConstants.name,
                    "=",
                    "BioModels"
            ).toArray()[0];
        } catch (Exception e) {
            logAndThrow("Unable to retrieve BioModels reference database", e);
        }

        if (biomodelsReferenceDatabase == null) {
            logger.info("Creating BioModels reference database - no existing one was found");
            biomodelsReferenceDatabase = new GKInstance();
            biomodelsReferenceDatabase.setDbAdaptor(dba);
            SchemaClass referenceDatabase = dba.getSchema().getClassByName(ReactomeJavaConstants.ReferenceDatabase);
            biomodelsReferenceDatabase.setSchemaClass(referenceDatabase);

            try {
                biomodelsReferenceDatabase.addAttributeValue(ReactomeJavaConstants.created, instanceEdit);
                biomodelsReferenceDatabase.addAttributeValue(
                        ReactomeJavaConstants.name, Arrays.asList("BioModels Database", "BioModels")
                );

                //TODO Check if both accessUrl and Url should be using https
                biomodelsReferenceDatabase.addAttributeValue(
                        ReactomeJavaConstants.accessUrl, "http://www.ebi.ac.uk/biomodels-main/publ-model.do?mid=###ID###"
                );
                biomodelsReferenceDatabase.addAttributeValue(
                        ReactomeJavaConstants.url, "https://www.ebi.ac.uk/biomodels/"
                );
                dba.storeInstance(biomodelsReferenceDatabase);
            } catch (Exception e) {
                logger.error("Unable to create BioModels reference database", e);
            }
        }
        logger.info("Successfully created BioModels reference database with db id of " +
                biomodelsReferenceDatabase.getDBID());

        return biomodelsReferenceDatabase;
    }

    public static void logAndThrow(String errorMessage, Throwable e) {
        logger.error(errorMessage, e);
        throw new RuntimeException(errorMessage, e);
    }
}
