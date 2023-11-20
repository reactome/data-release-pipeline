package org.reactome.release.orthoStableIdentifierHistory;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostStepChecks {

    private static Collection<GKInstance> stableIdentifierInstances = new ArrayList<>();
    private static Map<String, List<GKInstance>> identifierToStableIdentifierMap = new HashMap<>();
    private static Map<String, List<GKInstance>> oldIdentifierToStableIdentifierMap = new HashMap<>();
    private static MySQLAdaptor dbaCurator;
    private static final long HUMAN_INSTANCE_DBID = 48887L;
    private static final String NULL_SPECIES_PREFIX = "NUL";
    private static final String ALL_SPECIES_PREFIX = "ALL";

    public static void performStableIdentifierHistoryQA(MySQLAdaptor dba, MySQLAdaptor dbaC) throws Exception {

        dbaCurator = dbaC;

        Map<String, List<GKInstance>> identifiersAssociatedWithMultipleStableIdentifiersMap = findIdentifiersAssociatedWithMultipleStableIdentifierInstances(dba);
        Map<String, List<GKInstance>> oldIdentifiersAssociatedWithMultipleStableIdentifiersMap = findOldIdentifiersAssociatedWithMultipleStableIdentifierInstances(dba);

        for (GKInstance stableIdentifierInst : findStableIdentifierInstances(dba)) {
            Collection<GKInstance> stableIdentifierReferrals = stableIdentifierInst.getReferers(ReactomeJavaConstants.stableIdentifier);
            if (stableIdentifierReferrals == null) {
                System.out.println(stableIdentifierInst + " has null stableIdentifierReferrals");
            } else if (stableIdentifierReferrals.isEmpty()) {
                System.out.println(stableIdentifierInst + " has no referrers");
            } else if (stableIdentifierReferrals.size() > 1) {
                System.out.println(stableIdentifierInst + " has multiple referrers");
            } else {
                GKInstance referralInst = stableIdentifierReferrals.iterator().next();
                if (isInstanceRequiringStableIdentifier(referralInst) && hasIncorrectStableIdentifier(referralInst)) {
                    System.out.println(stableIdentifierInst + " is incorrect");
                }
            }

            String identifier = stableIdentifierInst.getAttributeValue(ReactomeJavaConstants.identifier).toString();
            if (identifiersAssociatedWithMultipleStableIdentifiersMap.get(identifier) != null) {
                System.out.println(stableIdentifierInst + " has an identifier that is used in multiple instances (" + identifier + ")");
            }

            if (stableIdentifierInst.getAttributeValue("oldIdentifier") != null) {
                String oldIdentifier = stableIdentifierInst.getAttributeValue("oldIdentifier").toString();
                if (!oldIdentifier.startsWith("REACT_")) {
                    System.out.println(stableIdentifierInst + " has an incorrect old stable identifier (" + oldIdentifier + ")");
                }

                if (oldIdentifiersAssociatedWithMultipleStableIdentifiersMap.get(oldIdentifier) != null) {
                    System.out.println(stableIdentifierInst + " has an old identifier that is used in multiple instances (" + oldIdentifier + ")");
                }
            }
        }

        for (GKInstance inst : getInstancesRequiringStableIdentifier(dba)) {
            if (inst.getAttributeValue(ReactomeJavaConstants.stableIdentifier) == null) {
                System.out.println(inst + " does not have a stable identifier");
            } else if (inst.getAttributeValuesList(ReactomeJavaConstants.stableIdentifier).size() > 1) {
                System.out.println(inst + " has multiple stable identifiers");
            }
        }

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
                Object identifierObj = stableIdentifierInst.getAttributeValue(ReactomeJavaConstants.identifier);
                String identifier = (identifierObj instanceof String) ? (String) identifierObj : null;

                if (identifier != null) {
                    if (identifierToStableIdentifierMap.containsKey(identifier)) {
                        List<GKInstance> existingList = identifierToStableIdentifierMap.get(identifier);
                        existingList.add(stableIdentifierInst);
                    } else {
                        List<GKInstance> stableIdentifierSingletonList = new ArrayList<>();
                        stableIdentifierSingletonList.add(stableIdentifierInst);
                        identifierToStableIdentifierMap.put(identifier, stableIdentifierSingletonList);
                    }
                }
            }
         }
         return identifierToStableIdentifierMap;
    }
    
    private static Map<String, List<GKInstance>> mapIdentifiersToStableIdentifiers(MySQLAdaptor dba) throws Exception {
        if (identifierToStableIdentifierMap.isEmpty()) {
            for (GKInstance stableIdentifierInst : findStableIdentifierInstances(dba)) {
                Object identifierObj = stableIdentifierInst.getAttributeValue(ReactomeJavaConstants.identifier);
                String identifier = (identifierObj instanceof String) ? (String) identifierObj : null;
                if (identifierToStableIdentifierMap.containsKey(identifier)) {
                    identifierToStableIdentifierMap.get(identifier).add(stableIdentifierInst);
                } else {
                    List<GKInstance> stableIdentifierSingletonList = new ArrayList<>(Arrays.asList(stableIdentifierInst));
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
                String oldIdentifier = (String) stableIdentifierInst.getAttributeValue("oldIdentifier");
                if (oldIdentifier != null) {
                    if (oldIdentifierToStableIdentifierMap.get(oldIdentifier) != null) {
                        oldIdentifierToStableIdentifierMap.get(oldIdentifier).add(stableIdentifierInst);
                    } else {
                        List<GKInstance> stableIdentifierSingletonList = new ArrayList<>();
                        stableIdentifierSingletonList.add(stableIdentifierInst);
                        oldIdentifierToStableIdentifierMap.put(oldIdentifier, stableIdentifierSingletonList);
                    }

                    List<GKInstance> tempList = new ArrayList<>(stableIdentifierList);  // Create a mutable list
                    tempList.add(stableIdentifierInst);  // Add the new instance to the list

                    oldIdentifierToStableIdentifierMap.put(oldIdentifier, tempList);  // Put the list back in the map
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

    private static boolean isInstanceRequiringStableIdentifier(GKInstance referralInst) {
        return referralInst.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity) || referralInst.getSchemClass().isa(ReactomeJavaConstants.Event);
    }

    private static boolean hasIncorrectStableIdentifier(GKInstance referralInst) throws Exception {
        return !hasCorrectIdentifierNumericComponent(referralInst) && !hasCorrectIdentifierSpeciesPrefix(referralInst);
    }

    private static boolean hasCorrectIdentifierNumericComponent(GKInstance referralInst) throws Exception {
        GKInstance stableIdentifierInst = (GKInstance) referralInst.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
        long stableIdentifierNumericComponent = getStableIdentifierNumericComponent(stableIdentifierInst);
        Set<Long> referralNumericComponent = getInstanceStableIdentifierNumericComponent(referralInst);
        return referralNumericComponent.contains(stableIdentifierNumericComponent);
    }

    private static long getStableIdentifierNumericComponent(GKInstance stableIdentifierInst) throws Exception {
        String identifier = stableIdentifierInst.getAttributeValue(ReactomeJavaConstants.identifier).toString();
        Pattern numericRegex = Pattern.compile("^R-\\w{3}-(\\d+)");
        Matcher numericMatch = numericRegex.matcher(identifier);
        // The match group isn't accessible unless the .find method loads it
        numericMatch.find();
        return Long.parseLong(numericMatch.group(1));
    }

    private static Set<Long> getInstanceStableIdentifierNumericComponent(GKInstance referralInst) throws Exception {
        Set<Long> dbids = new HashSet<>();
        if (isElectronicallyInferred(referralInst)) {
             dbids = getSourceSpeciesDbIds(referralInst);
        } else {
            dbids.add(referralInst.getDBID());
        }
        return dbids;
    }

    // Checks if the instance was generated electronically. Event-type instances can be easily checked through the
    // the EvidenceType attribute. PhysicalEntity-type instances are trickier, due to the lack of any attribute that
    // can inform whether the instance was electronically generated or not. For now, we try and pull the same instance
    // from the curator database by DBID. Instances that exist in both DBs are assumed to have been created manually.
    private static boolean isElectronicallyInferred(GKInstance referralInst) throws Exception {

        if (referralInst.getSchemClass().isa(ReactomeJavaConstants.Event)) {
            return referralInst.getAttributeValue(ReactomeJavaConstants.evidenceType) != null;
        } else {
            // Find out if PhysicalEntity is electronically inferred
            GKInstance curatorInst = dbaCurator.fetchInstance(referralInst.getDBID());

            return !inCuratorDatabase(referralInst, curatorInst) && hasHumanSourceInstance(referralInst);
        }
    }

    private static boolean inCuratorDatabase(GKInstance referralInst, GKInstance curatorInst) throws Exception {
        return curatorInst != null &&
                referralInst.getSchemClass().getName().equals(curatorInst.getSchemClass().getName()) &&
                referralInst.getDisplayName().equals(curatorInst.getDisplayName()) &&
                haveSameSpeciesValue(referralInst, curatorInst);
    }

    // Checks that the species attribute, if it exists, is equal between the instances from curator DB and release DB.
    // If species is a valid attribute for the SchemaClass of the instances (which the isElectronicallyInferred method
    // has confirmed are the same if we've reached this point), then we need to check if the returned Species instance
    // is the same between the two instances. It is valid if they are both null (handled in bothValuesAreEqual method).
    private static boolean haveSameSpeciesValue(GKInstance referralInst, GKInstance curatorInst) throws Exception {
        if (!referralInst.getSchemClass().isValidAttribute(ReactomeJavaConstants.species)) {
            return true;
        }

        GKInstance referralSpecies = (GKInstance) referralInst.getAttributeValue(ReactomeJavaConstants.species);
        GKInstance curatorSpecies = (GKInstance) curatorInst.getAttributeValue(ReactomeJavaConstants.species);

        return bothValuesAreEqual(referralSpecies, curatorSpecies);
    }

    // We want to return true in two cases: 1) Both instances are null or 2) Both instances are the same.
    // We can't check equality using .equals on a null value, so first check if both are null, second check if only
    // one is null, and third compare display names between the two GKInstances.
    private static boolean bothValuesAreEqual(GKInstance firstInst, GKInstance secondInst) {
        if (firstInst == null && secondInst == null) {
            return true;
        }
        if (firstInst == null || secondInst == null) {
            return false;
        }

        return firstInst.getDisplayName().equals(secondInst.getDisplayName());
    }

    private static boolean hasHumanSourceInstance(GKInstance referralInst) throws Exception {

        GKInstance inferredFromInst = (GKInstance) referralInst.getAttributeValue(ReactomeJavaConstants.inferredFrom);
        if (inferredFromInst != null) {
            GKInstance speciesInst = (GKInstance) inferredFromInst.getAttributeValue(ReactomeJavaConstants.species);
            return speciesInst != null && speciesInst.getDBID().equals(HUMAN_INSTANCE_DBID);
        }
        return false;
    }

    private static Set<Long> getSourceSpeciesDbIds(GKInstance referralInst) throws Exception {
        Collection<GKInstance> inferredFromInstances = referralInst.getAttributeValuesList(ReactomeJavaConstants.inferredFrom);
        Collection<GKInstance> inferredToInstances = referralInst.getReferers(ReactomeJavaConstants.inferredTo);
        Set<Long> dbids = new HashSet<>();
        for (GKInstance inferredFromInst : inferredFromInstances) {
            dbids.add(inferredFromInst.getDBID());
        }
        if (inferredToInstances != null) {
            for (GKInstance inferredToInst : inferredToInstances) {
                dbids.add(inferredToInst.getDBID());
            }
        }
        return dbids;
    }

    private static boolean hasCorrectIdentifierSpeciesPrefix(GKInstance referralInst) throws Exception {
        String instanceSpeciesPrefix = getInstanceSpeciesPrefix(referralInst);
        GKInstance stableIdentifierInst = (GKInstance) referralInst.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
        String stableIdentifierSpeciesPrefix = getStableIdentifierSpeciesPrefix(stableIdentifierInst);
        return instanceSpeciesPrefix.equals(stableIdentifierSpeciesPrefix);


    }

    private static String getInstanceSpeciesPrefix(GKInstance referralInst) throws Exception {
        String speciesPrefix;
        if (referralInst.getSchemClass().isa(ReactomeJavaConstants.Event)) {
            speciesPrefix = getSpeciesPrefixFromEvent(referralInst);
        } else {
            speciesPrefix = getSpeciesPrefixFromPhysicalEntity(referralInst);
        }
        return speciesPrefix;
    }

    private static String getSpeciesPrefixFromEvent(GKInstance referralInst) throws Exception {
        Collection<GKInstance> speciesList = referralInst.getAttributeValuesList(ReactomeJavaConstants.species);
        String speciesPrefix;
        if (speciesList.size() == 1) {
            speciesPrefix = getSpeciesPrefix(speciesList.iterator().next());
        } else {
            speciesPrefix = NULL_SPECIES_PREFIX;
        }
        return speciesPrefix;
    }

    private static String getSpeciesPrefix(GKInstance speciesInst) throws Exception {
        return speciesInst.getAttributeValue(ReactomeJavaConstants.abbreviation).toString();
    }

    private static String getSpeciesPrefixFromPhysicalEntity(GKInstance referralInst) throws Exception {
        String speciesPrefix;
        if (referralInst.getSchemClass().isValidAttribute(ReactomeJavaConstants.species)) {
            Collection<GKInstance> speciesList = referralInst.getAttributeValuesList(ReactomeJavaConstants.species);
            if (speciesList.size() > 1) {
                speciesPrefix = NULL_SPECIES_PREFIX;
            } else if (speciesList.size() == 1) {
                speciesPrefix = getSpeciesPrefix(speciesList.iterator().next());
            } else {
                Set<GKInstance> speciesAssociatedWithPhysicalEntity = getUniqueSpeciesFromAllEntities(referralInst);
                if (speciesAssociatedWithPhysicalEntity.size() > 1) {
                    speciesPrefix = NULL_SPECIES_PREFIX;
                } else if (speciesAssociatedWithPhysicalEntity.size() == 1) {
                    speciesPrefix = getSpeciesPrefix(speciesAssociatedWithPhysicalEntity.iterator().next());
                } else {
                    speciesPrefix = ALL_SPECIES_PREFIX;
                }
            }
        } else {
            speciesPrefix = ALL_SPECIES_PREFIX;
        }
        return speciesPrefix;
    }

    private static Set<GKInstance> getUniqueSpeciesFromAllEntities(GKInstance referralInst) throws Exception {
        Set<GKInstance> constituentInstances = findConstituentEntities(referralInst);
        Set<GKInstance> speciesInstances = new HashSet<>();
        for (GKInstance constituentInst : constituentInstances) {
            if (constituentInst.getSchemClass().isValidAttribute(ReactomeJavaConstants.species)) {
                speciesInstances.addAll(constituentInst.getAttributeValuesList(ReactomeJavaConstants.species));
            }
        }
        return speciesInstances;
    }

    private static Set<GKInstance> findConstituentEntities(GKInstance inst) throws Exception {
        Set<GKInstance> constituentEntities = new HashSet<>(Arrays.asList(inst));
        if (inst.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
            Collection<GKInstance> componentInstances = inst.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
            for (GKInstance componentInst : componentInstances) {
                constituentEntities.addAll(findConstituentEntities(componentInst));
            }
        } else if (inst.getSchemClass().isa(ReactomeJavaConstants.Polymer)) {
            Collection<GKInstance> repeatedUnitInstances = inst.getAttributeValuesList(ReactomeJavaConstants.repeatedUnit);
            for (GKInstance repeatedUnitInst : repeatedUnitInstances) {
                constituentEntities.addAll(findConstituentEntities(repeatedUnitInst));
            }
        } else if (inst.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
            Collection<GKInstance> memberAndCandidateInstances = inst.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasCandidate)) {
                memberAndCandidateInstances.addAll(inst.getAttributeValuesList(ReactomeJavaConstants.hasCandidate));
            }
            for (GKInstance memberOrCandidateInst : memberAndCandidateInstances) {
                constituentEntities.addAll(findConstituentEntities(memberOrCandidateInst));
            }
        }
        return constituentEntities;
    }

    private static String getStableIdentifierSpeciesPrefix(GKInstance stableIdentifierInst) throws Exception {
        String identifier = stableIdentifierInst.getAttributeValue(ReactomeJavaConstants.identifier).toString();
        Pattern numericRegex = Pattern.compile("^R-(\\w{3})-\\d+");
        Matcher numericMatch = numericRegex.matcher(identifier);
        // The match group isn't accessible unless the .find method loads it
        numericMatch.find();
        return numericMatch.group(1);
    }

    private static Collection<GKInstance> getInstancesRequiringStableIdentifier(MySQLAdaptor dba) throws Exception {
        Collection<GKInstance> physicalEntityAndEventInstances = dba.fetchInstancesByClass(ReactomeJavaConstants.Event);
        physicalEntityAndEventInstances.addAll(dba.fetchInstancesByClass(ReactomeJavaConstants.PhysicalEntity));
        return physicalEntityAndEventInstances;
    }

}
