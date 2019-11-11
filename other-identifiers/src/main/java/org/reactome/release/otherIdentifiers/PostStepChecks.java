package org.reactome.release.otherIdentifiers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

import java.util.Collection;
import java.util.List;

/**
 *  This is a QA step that will be run after finishing an OtherIdentifiers run. For each species,
 *  the count of ReferenceGeneProduct instances that have their 'otherIdentifier' attribute filled is found
 *  between the current and previous database. If the previous database has a higher count, a warning message
 *  is output and should be looked into. If the count is still relatively close,  it can probably be ignored.
 */

public class PostStepChecks {
    private static final Logger logger = LogManager.getLogger();

    public static void compareOtherIdentifierCounts(MySQLAdaptor dba, MySQLAdaptor dbaPrev) throws Exception {

        Collection<GKInstance> speciesInstances = dba.fetchInstancesByClass(ReactomeJavaConstants.Species);
        int currentOtherIdentifierCount = 0;
        int previousOtherIdentifierCount = 0;
        for (GKInstance speciesInstance : speciesInstances) {
            long speciesCurrentOtherIdentifierCount = getRGPInstanceOtherIdentifierCount(dba, speciesInstance);
            long speciesPreviousOtherIdentifierCount = getRGPInstanceOtherIdentifierCount(dbaPrev, speciesInstance);
            if (speciesCurrentOtherIdentifierCount < speciesPreviousOtherIdentifierCount) {
                logger.warn(speciesInstance.getDisplayName() + " has fewer ReferenceGeneProduct instances with OtherIdentifiers compared to previous release:  " +
                        dba.getDBName() + " - " + speciesCurrentOtherIdentifierCount + " -- " + dbaPrev.getDBName() + " - " + speciesPreviousOtherIdentifierCount);
            }
            currentOtherIdentifierCount += speciesCurrentOtherIdentifierCount;
            previousOtherIdentifierCount += speciesPreviousOtherIdentifierCount;
        }

        if (currentOtherIdentifierCount < previousOtherIdentifierCount) {
            logger.warn(dba.getDBName() + " has fewer ReferenceGeneProduct instances compared to " + dbaPrev.getDBName() + ": " +
                    dba.getDBName() + " - " + currentOtherIdentifierCount + " -- " + dbaPrev.getDBName() + " - " + previousOtherIdentifierCount);
        }
    }

    /**
     * Wrapper method for determining other identifier count.
     * @param dba MySQLAdaptor
     * @param speciesInstance GKInstance pertaining to a specific species
     * @return The other identifier count, as an int.
     * @throws Exception can be caused by errors interacting with the database using the db adaptor.
     */
    private static long getRGPInstanceOtherIdentifierCount(MySQLAdaptor dba, GKInstance speciesInstance) throws Exception {
        Collection<GKInstance> rgpInstances = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct, ReactomeJavaConstants.species, "=", speciesInstance);
        return getCountOfInstancesWithOtherIdentifiers(rgpInstances);
    }

    /**
     * Determines the number of ReferenceGeneProduct instances that have a filled otherIdentifier attribute.
     * @param rgpInstances Collection of RGP GKInstances associated with a species.
     * @return An int is returned, representing the otherIdentifier count.
     */
    public static long getCountOfInstancesWithOtherIdentifiers(Collection<GKInstance> rgpInstances) {
        return rgpInstances.stream().filter(rgp -> hasOtherIdentifiers(rgp)).count();
    }

    /**
     * Returns true if the ReferenceGeneProduct instance has a filled otherIdentifier attribute, false if not.
     * @param rgpInstance GKInstance for ReferenceGeneProduct
     * @return boolean
     */
    public static boolean hasOtherIdentifiers(GKInstance rgpInstance) {
        try {
            List<String> otherIdentifiers = rgpInstance.getAttributeValuesList(ReactomeJavaConstants.otherIdentifier);

            return !otherIdentifiers.isEmpty();
        } catch(Exception e) {
            String errorMessage = "Unable to retrieve other identifiers from RGP instance " + rgpInstance;

            logger.fatal(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }
}
