package org.reactome.release.otherIdentifiers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class PostStepChecks {
    private static final Logger logger = LogManager.getLogger();

    public static void compareOtherIdentifierCounts(MySQLAdaptor dba, MySQLAdaptor dbaPrev) throws Exception {

        Collection<GKInstance> speciesInstances = dba.fetchInstancesByClass(ReactomeJavaConstants.Species);

        for (GKInstance speciesInstance : speciesInstances) {
            int currentOtherIdentifierCount = getOtherIdentifierCount(dba, speciesInstance);
            int previousOtherIdentifierCount = getOtherIdentifierCount(dbaPrev, speciesInstance);
            if (currentOtherIdentifierCount < previousOtherIdentifierCount) {
                logger.warn(speciesInstance.getDisplayName() + " has fewer ReferenceGeneProduct instances with OtherIdentifiers compared to previous release:  " +
                        "release_current - " + currentOtherIdentifierCount + " -- release_previous - " + previousOtherIdentifierCount);
            }
        }


    }

    private static int getOtherIdentifierCount(MySQLAdaptor dba, GKInstance speciesInstance) throws Exception {
        AtomicInteger currentOtherIdentifierCount = new AtomicInteger();
        Collection<GKInstance> rgpInstances = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct, ReactomeJavaConstants.species, "=", speciesInstance);
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
