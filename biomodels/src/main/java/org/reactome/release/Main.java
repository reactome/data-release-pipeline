package org.reactome.release;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jweiser
 *
 */
public class Main {
    private static final Logger logger = LogManager.getLogger();

    /**
     * @param args Command line arguments for the biomodels program
     * @throws SQLException Thrown if unable to connect to the configured Reactome database
     */
    public static void main(String[] args) throws SQLException {
        logger.info("Running BioModels insertion");

        String pathToResources = args.length > 0 ? args[0] : "biomodels/src/main/resources/config.properties";

        Properties props = new Properties();
        try {
            props.load(new FileInputStream(pathToResources));
        } catch (IOException e) {
            e.printStackTrace();
        }

        MySQLAdaptor dba = getDBA(props);

        // Create new instanceEdit in database to track modified pathways
        long personId = Long.parseLong(props.getProperty("personId"));
        GKInstance instanceEdit = createInstanceEdit(dba, personId, "BioModels reference database creation");

        Map<String, List<String>> pathwayStableIdToBiomodelsIds =
            ModelsTSVParser.parse("biomodels/src/main/resources/models2pathways.tsv");

        for (GKInstance pathway: getPathwaysWithBiomodelsIds(dba, pathwayStableIdToBiomodelsIds.keySet())) {
            logger.info("Adding BioModels ids to pathway " + pathway.getExtendedDisplayName());
            List<String> biomodelsIds = pathwayStableIdToBiomodelsIds.get(getStableIdentifier(pathway));
            List<GKInstance> biomodelsDatabaseIdentifiers =
                createBiomodelsDatabaseIdentifiers(biomodelsIds, instanceEdit, dba);

            try {
                pathway.addAttributeValue(ReactomeJavaConstants.crossReference, biomodelsDatabaseIdentifiers);
                pathway.addAttributeValue(ReactomeJavaConstants.modified, instanceEdit);
                dba.updateInstanceAttribute(pathway, ReactomeJavaConstants.crossReference);
                dba.updateInstanceAttribute(pathway, ReactomeJavaConstants.modified);
            } catch (Exception e) {
                logger.error("Unable to update pathway " + pathway.getExtendedDisplayName() +
                        " with BioModels ids " + biomodelsIds, e);
                System.exit(1);
            }

            logger.info("BioModels ids successfully added to pathway " + pathway.getExtendedDisplayName());
        }
        logger.info("Completed BioModels insertion");
    }

    private static MySQLAdaptor getDBA(Properties props) throws SQLException {
        String host = props.getProperty("host");
        String database = props.getProperty("db");
        String user = props.getProperty("user");
        String password = props.getProperty("pass");
        int port = Integer.parseInt(props.getProperty("port"));

        return new MySQLAdaptor(host, database, user, password, port);
    }

    @SuppressWarnings("unchecked")
    private static List<GKInstance> getPathways(MySQLAdaptor dba) {
        List<GKInstance> pathways = new ArrayList<>();

        try {
            pathways.addAll(dba.fetchInstancesByClass(ReactomeJavaConstants.Pathway));
        } catch (Exception e) {
            logAndThrow("Unable to fetch pathways from database", e);
        }

        return pathways;
    }

    private static List<GKInstance> createBiomodelsDatabaseIdentifiers(List<String> biomodelsIds,
                                                              GKInstance instanceEdit, MySQLAdaptor dba) {
        List<GKInstance> biomodelsDatabaseIdentifiers = new ArrayList<>();

        for (String biomodelsId : biomodelsIds) {
            logger.info("Creating database identifier for BioModels id " + biomodelsId);

            GKInstance biomodelsDatabaseIdentifier = new GKInstance();
            biomodelsDatabaseIdentifier.setDbAdaptor(dba);
            SchemaClass databaseIdentifierSchemaClass =
                dba.getSchema().getClassByName(ReactomeJavaConstants.DatabaseIdentifier);
            biomodelsDatabaseIdentifier.setSchemaClass(databaseIdentifierSchemaClass);

            try {
                biomodelsDatabaseIdentifier.addAttributeValue(ReactomeJavaConstants.created, instanceEdit);
                biomodelsDatabaseIdentifier.addAttributeValue(ReactomeJavaConstants.identifier, biomodelsId);
                biomodelsDatabaseIdentifier.addAttributeValue(ReactomeJavaConstants.referenceDatabase,
                        fetchBiomodelsReferenceDatabase(dba, instanceEdit));
                InstanceDisplayNameGenerator.setDisplayName(biomodelsDatabaseIdentifier);
            } catch (Exception e) {
                logAndThrow("Unable to create BioModels database identifier for " + biomodelsId, e);
            }

            biomodelsDatabaseIdentifiers.add(biomodelsDatabaseIdentifier);
            logger.info("Successfully created database identifier for BioModels id " + biomodelsId);
        }

        return biomodelsDatabaseIdentifiers;
    }

    private static Iterable<? extends GKInstance> getPathwaysWithBiomodelsIds(MySQLAdaptor dba,
                                                                              Set<String> pathwayStableIds) {
        return getPathways(dba)
                .stream()
                .filter(pathway -> pathwayStableIds.contains(getStableIdentifier(pathway)))
                .collect(Collectors.toList());
    }

    private static GKInstance fetchBiomodelsReferenceDatabase(MySQLAdaptor dba, GKInstance instanceEdit) {
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

    private static String getStableIdentifier(GKInstance instance) {
        String stableIdentifier = "";

        try {
            GKInstance stableIdentifierInstance =
                    (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
            stableIdentifier = (String) stableIdentifierInstance.getAttributeValue(ReactomeJavaConstants.identifier);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return stableIdentifier;
    }

    private static GKInstance createInstanceEdit(MySQLAdaptor dba, long defaultPersonId, String note) {
        logger.info("Creating new instance edit for person id " + defaultPersonId);

        GKInstance defaultPerson = null;
        try {
            defaultPerson = dba.fetchInstance(defaultPersonId);
        } catch (Exception e) {
            logAndThrow("Could not fetch Person entity with ID " + defaultPersonId +
                ". Please check that a Person entity exists in the database with this ID", e);
        }

        GKInstance newIE = null;
        try {
            newIE = createDefaultInstanceEdit(defaultPerson);
            newIE.addAttributeValue(ReactomeJavaConstants.dateTime, GKApplicationUtilities.getDateTime());
            newIE.addAttributeValue(ReactomeJavaConstants.note, note);
        } catch (InvalidAttributeException | InvalidAttributeValueException e) {
            logAndThrow("Unable to create instance edit", e);
        }
        InstanceDisplayNameGenerator.setDisplayName(newIE);

        try {
            dba.storeInstance(newIE);
        } catch (Exception e) {
            logAndThrow("Unable to store new instance edit", e);
        }
        logger.info("Successfully created new instance edit with db id " + newIE.getDBID() + " for person id " +
            defaultPersonId);

        return newIE;
    }

    private static GKInstance createDefaultInstanceEdit(GKInstance person)
            throws InvalidAttributeException, InvalidAttributeValueException {
        GKInstance instanceEdit = new GKInstance();
        PersistenceAdaptor adaptor = person.getDbAdaptor();
        instanceEdit.setDbAdaptor(adaptor);
        SchemaClass cls = adaptor.getSchema().getClassByName(ReactomeJavaConstants.InstanceEdit);
        instanceEdit.setSchemaClass(cls);

        instanceEdit.addAttributeValue(ReactomeJavaConstants.author, person);

        return instanceEdit;
    }

    private static void logAndThrow(String errorMessage, Throwable e) {
        logger.error(errorMessage, e);
        throw new RuntimeException(errorMessage, e);
    }
}
