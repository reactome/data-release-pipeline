package org.reactome.release.otherIdentifiers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

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

        for (GKInstance speciesInstance : speciesInstances) {
            int currentOtherIdentifierCount = getRGPInstanceOtherIdentifierCount(dba, speciesInstance);
            int previousOtherIdentifierCount = getRGPInstanceOtherIdentifierCount(dbaPrev, speciesInstance);
            if (currentOtherIdentifierCount < previousOtherIdentifierCount) {
                logger.warn(speciesInstance.getDisplayName() + " has fewer ReferenceGeneProduct instances with OtherIdentifiers compared to previous release:  " +
                        "release_current - " + currentOtherIdentifierCount + " -- release_previous - " + previousOtherIdentifierCount);
            }
        }
    }

    /**
     * Wrapper method for determining other identifier count.
     * @param dba MySQLAdaptor
     * @param speciesInstance GKInstance pertaining to a specific species
     * @return The other identifier count, as an int.
     * @throws Exception
     */
    private static int getRGPInstanceOtherIdentifierCount(MySQLAdaptor dba, GKInstance speciesInstance) throws Exception {
        Collection<GKInstance> rgpInstances = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct, ReactomeJavaConstants.species, "=", speciesInstance);

        int currentOtherIdentifierCount = getOtherIdentifierCount(rgpInstances);
        return currentOtherIdentifierCount;
    }

    /**
     * Determines the number of ReferenceGeneProduct instances that have a filled otherIdentifier attribute.
     * @param rgpInstances Collection of RGP GKInstances associated with a species.
     * @return An int is returned, representing the otherIdentifier count.
     */
    public static int getOtherIdentifierCount(Collection<GKInstance> rgpInstances) {
        AtomicInteger currentOtherIdentifierCount = new AtomicInteger();
        rgpInstances.forEach(rgpInstance -> {
            try {
                if (rgpInstance.getAttributeValuesList(ReactomeJavaConstants.otherIdentifier).size() > 0) {
                    currentOtherIdentifierCount.getAndIncrement();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return currentOtherIdentifierCount.intValue();
    }
}
