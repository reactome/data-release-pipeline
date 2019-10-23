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

    private static Map<String, GKInstance> biomodelsInstances = new HashMap<>();
    private static final Logger logger = LogManager.getLogger();

    /**
     * @param args Command line arguments for the biomodels program
     * @throws SQLException Thrown if unable to connect to the configured Reactome database
     */
    public static void main(String[] args) throws SQLException {
        logger.info("Running BioModels insertion");

        String pathToResources = args.length > 0 ? args[0] : "src/main/resources/config.properties";
        String pathToModels2Pathways = args.length > 1 ? args[1] : "src/main/resources/models2pathways.tsv";

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
            ModelsTSVParser.parse(pathToModels2Pathways);

        for (GKInstance pathway: getPathwaysWithBiomodelsIds(dba, pathwayStableIdToBiomodelsIds.keySet())) {
            logger.info("Adding BioModels ids to pathway " + pathway.getExtendedDisplayName());
            List<String> biomodelsIds = pathwayStableIdToBiomodelsIds.get(getStableIdentifier(pathway));
            List<GKInstance> biomodelsDatabaseIdentifiers =
                createBiomodelsDatabaseIdentifiers(biomodelsIds, instanceEdit, dba);

            try {
                pathway.addAttributeValue(ReactomeJavaConstants.crossReference, biomodelsDatabaseIdentifiers);
                Collection<GKInstance> modifiedInstances = pathway.getAttributeValuesList(ReactomeJavaConstants.modified);
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
        String host = props.getProperty("release.database.host");
        String database = props.getProperty("release_current.name");
        String user = props.getProperty("release.database.user");
        String password = props.getProperty("release.database.password");
        int port = Integer.parseInt(props.getProperty("release.database.port"));

        return new MySQLAdaptor(host, database, user, password, port);
    }

    @SuppressWarnings("unchecked")
    private static List<GKInstance> getPathways(MySQLAdaptor dba) {
        List<GKInstance> pathways = new ArrayList<>();

        try {
            pathways.addAll(dba.fetchInstancesByClass(ReactomeJavaConstants.Pathway));
        } catch (Exception e) {
            BioModelsUtilities.logAndThrow("Unable to fetch pathways from database", e);
        }

        return pathways;
    }

    /**
     * Creates a DatabaseIdentifier instance for the biomodel identifier, if it hasn't already been created during the current run.
     * @param biomodelsIds -- List of BioModels IDs
     * @param instanceEdit -- GKInstance instanceEdit attached to the person Id that is executing this program
     * @param dba -- MySQLAdaptor
     * @return -- Returns a list of DatabaseIdentifier objects pertaining to the BioModel identifier
     */
    private static List<GKInstance> createBiomodelsDatabaseIdentifiers(List<String> biomodelsIds,
                                                              GKInstance instanceEdit, MySQLAdaptor dba) {

        List<GKInstance> biomodelsDatabaseIdentifiers = new ArrayList<>();

        for (String biomodelsId : biomodelsIds) {

            // If the identifier already had an object created during this run, use that. Otherwise create one.
            if (biomodelsInstances.get(biomodelsId) != null) {
                biomodelsDatabaseIdentifiers.add(biomodelsInstances.get(biomodelsId));
            } else {
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
                            BioModelsUtilities.fetchBiomodelsReferenceDatabase(dba, instanceEdit));
                    InstanceDisplayNameGenerator.setDisplayName(biomodelsDatabaseIdentifier);
                } catch (Exception e) {
                    BioModelsUtilities.logAndThrow("Unable to create BioModels database identifier for " + biomodelsId, e);
                }

                biomodelsDatabaseIdentifiers.add(biomodelsDatabaseIdentifier);
                biomodelsInstances.put(biomodelsId, biomodelsDatabaseIdentifier);
                logger.info("Successfully created database identifier for BioModels id " + biomodelsId);
            }
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
            BioModelsUtilities.logAndThrow("Could not fetch Person entity with ID " + defaultPersonId +
                ". Please check that a Person entity exists in the database with this ID", e);
        }

        GKInstance newIE = null;
        try {
            newIE = createDefaultInstanceEdit(defaultPerson);
            newIE.addAttributeValue(ReactomeJavaConstants.dateTime, GKApplicationUtilities.getDateTime());
            newIE.addAttributeValue(ReactomeJavaConstants.note, note);
        } catch (InvalidAttributeException | InvalidAttributeValueException e) {
            BioModelsUtilities.logAndThrow("Unable to create instance edit", e);
        }
        InstanceDisplayNameGenerator.setDisplayName(newIE);

        try {
            dba.storeInstance(newIE);
        } catch (Exception e) {
            BioModelsUtilities.logAndThrow("Unable to store new instance edit", e);
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
}
