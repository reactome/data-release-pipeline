package org.reactome.release.orthoStableIdentifierHistory;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

import java.util.*;

public class PostStepChecks {

    private static Collection<GKInstance> stableIdentifierInstances = new ArrayList<>();
    private static Map<String, List<GKInstance>> identifierToStableIdentifierMap = new HashMap<>();
    private static Map<String, List<GKInstance>> oldIdentifierToStableIdentifierMap = new HashMap<>();

    public static void performStableIdentifierHistoryQA(MySQLAdaptor dba) throws Exception {
        Map<String, List<GKInstance>> identifiersAssociatedWithMultipleStableIdentifiersMap = findIdentifiersAssociatedWithMultipleStableIdentifierInstances(dba);
        Map<String, List<GKInstance>> oldIdentifiersAssociatedWithMultipleStableIdentifiersMap = findOldIdentifiersAssociatedWithMultipleStableIdentifierInstances(dba);
    }
    
    private static Map<String, List<GKInstance>> findIdentifiersAssociatedWithMultipleStableIdentifierInstances(MySQLAdaptor dba) throws Exception {
        identifierToStableIdentifierMap = mapIdentifiersToStableIdentifiers(dba);
        Map<String, List<GKInstance>> identifiersAssociatedWithMultipleStableIdentifiers = new HashMap<>();
        for (String identifier : identifierToStableIdentifierMap.keySet()) {
            if (identifierToStableIdentifierMap.get(identifier).size() > 1) {
                identifiersAssociatedWithMultipleStableIdentifiers.put(identifier, identifierToStableIdentifierMap.get(identifier));
            }
        }
        return identifiersAssociatedWithMultipleStableIdentifiers;
    }

    private static Map<String, List<GKInstance>> mapIdentifiersToStableIdentifiers(MySQLAdaptor dba) throws Exception {
        if (identifierToStableIdentifierMap.isEmpty()) {
            for (GKInstance stableIdentifierInst : findStableIdentifierInstances(dba)) {
                String identifier = stableIdentifierInst.getAttributeValue(ReactomeJavaConstants.identifier).toString();
                if (identifierToStableIdentifierMap.get(identifier) != null) {
                    identifierToStableIdentifierMap.get(identifier).add(stableIdentifierInst);
                } else {
                    List<GKInstance> stableIdentifierSingletonList = Arrays.asList(stableIdentifierInst);
                    identifierToStableIdentifierMap.put(identifier, stableIdentifierSingletonList);
                }
            }
        }
        return identifierToStableIdentifierMap;
    }

    private static Map<String, List<GKInstance>> findOldIdentifiersAssociatedWithMultipleStableIdentifierInstances(MySQLAdaptor dba) throws Exception {
        oldIdentifierToStableIdentifierMap = mapOldIdentifiersToStableIdentifiers(dba);
        Map<String, List<GKInstance>> oldIdentifiersAssociatedWithMultipleStableIdentifiers = new HashMap<>();
        for (String oldIdentifier : oldIdentifierToStableIdentifierMap.keySet()) {
            if (oldIdentifierToStableIdentifierMap.get(oldIdentifier).size() > 1) {
                oldIdentifiersAssociatedWithMultipleStableIdentifiers.put(oldIdentifier, oldIdentifierToStableIdentifierMap.get(oldIdentifier));
            }
        }
        return oldIdentifiersAssociatedWithMultipleStableIdentifiers;
    }

    private static Map<String, List<GKInstance>> mapOldIdentifiersToStableIdentifiers(MySQLAdaptor dba) throws Exception {
        if (oldIdentifierToStableIdentifierMap.isEmpty()) {
            for (GKInstance stableIdentifierInst : findStableIdentifierInstances(dba)) {
                // Cast to String instead of using toString() in case value returned is null.
                String oldIdentifier = (String) stableIdentifierInst.getAttributeValue("oldIdentifier");
                if (oldIdentifier != null) {
                    if (oldIdentifierToStableIdentifierMap.get(oldIdentifier) != null) {
                        oldIdentifierToStableIdentifierMap.get(oldIdentifier).add(stableIdentifierInst);
                    } else {
                        List<GKInstance> stableIdentifierSingletonList = Arrays.asList(stableIdentifierInst);
                        oldIdentifierToStableIdentifierMap.put(oldIdentifier, stableIdentifierSingletonList);
                    }
                }
            }
        }
        return oldIdentifierToStableIdentifierMap;
    }

    private static Collection<GKInstance> findStableIdentifierInstances(MySQLAdaptor dba) throws Exception {

        if (stableIdentifierInstances.isEmpty()) {
            stableIdentifierInstances = dba.fetchInstancesByClass(ReactomeJavaConstants.StableIdentifier);
        }
        return stableIdentifierInstances;
    }
}
