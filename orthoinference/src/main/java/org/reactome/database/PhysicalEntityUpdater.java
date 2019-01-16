package org.reactome.database;

import java.io.FileInputStream;
import java.util.*;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.*;

public class PhysicalEntityUpdater {
	private static final List<String> subunitAttributes = new ArrayList<>(Arrays.asList("hasMember", "hasCandidate", "hasComponent", "repeatedUnit"));
	public static void main(String[] args) throws Exception {

		String pathToConfig = "src/main/resources/config.properties";
		Properties props = new Properties();
		props.load(new FileInputStream(pathToConfig));
		
		String username = props.getProperty("username");
		String password = props.getProperty("password");
		String host = props.getProperty("host");
		String database = props.getProperty("database");
		int port = Integer.valueOf(props.getProperty("port"));
		
		MySQLAdaptor dba = new MySQLAdaptor(host, "fat_test_reactome_67", username, password, port);
		Collection<GKInstance> physicalEntityInstances = dba.fetchInstancesByClass("PhysicalEntity");
		Map<String,Map<String,Map<String,Integer>>> referralsMap = new HashMap<>();
		
		// Iterate through each PE
		int orphanCount = 0;
		List<GKInstance> orphanPhysicalEntitys = new ArrayList<>();
		for (GKInstance physicalEntityInst : physicalEntityInstances) {
			// Referrals need to be checked on a per-attribute basis. We get the total number of referrals
			// across all attributes for the instance, and store any attributes with a referral in a Set.
			int referralCount = 0;
			Set<String> referralAttributes = new HashSet<>();
			for (Object attributeReferral : physicalEntityInst.getSchemClass().getReferers()) {
				GKSchemaAttribute gkSA = (GKSchemaAttribute) attributeReferral;
				Collection<GKInstance> instanceReferralsByAttribute = physicalEntityInst.getReferers(gkSA);
				referralCount += instanceReferralsByAttribute.size();
				if (instanceReferralsByAttribute.size() > 0) {
					referralAttributes.add(gkSA.getName());

				}
			}
			
			// If only a single referral exists, and it is connected via the 'inferredTo' attribute,
			// this means the instance is an orphan -- not connected to any Pathway or Reaction, and
			// generated from a failed inference attempt -- and can be removed.
			if (referralCount == 1 && referralAttributes.contains("inferredTo")) {
				orphanCount++;
				orphanPhysicalEntitys.add(physicalEntityInst);
			}
		}

		for (GKInstance orphanEntityInst : orphanPhysicalEntitys) {
			SchemaClass schemClass = orphanEntityInst.getSchemClass();
			if (schemClass.isa("EntitySet") || schemClass.isa("Complex") ||  schemClass.isa("Polymer")) {
				recurseChildInstancesAndDelete(orphanEntityInst, 0);
				System.out.println("Deleting parent instance " + orphanEntityInst.getDBID());
			} else {
//				System.out.println("Not an EntitySet, Complex or Polymer -- Only Referral is an 'inferredTo' -- Deleting");
				System.out.println("Deleting parent instance " + orphanEntityInst.getDBID());
			}
		}
	}

	private static void recurseChildInstancesAndDelete(GKInstance orphanEntityInst, int recursionCount) throws Exception {

		System.out.println(orphanEntityInst);
		Set<String> validSubunitAttributesForInstance = new HashSet<>(Arrays.asList("inferredTo"));
		Set<GKInstance> childInstances = new HashSet<>();

		// This finds all valid attributes that contain child instances for the instance.
		// This could be hasMember/hasCandidate (EntitySet), hasComponent (Complex), or repeatedUnit (Polymer).
		for (String  subunitAttribute : subunitAttributes) {
			if (orphanEntityInst.getSchemClass().isValidAttribute(subunitAttribute)) {
				validSubunitAttributesForInstance.add(subunitAttribute);
				childInstances.addAll(orphanEntityInst.getAttributeValuesList(subunitAttribute));
			}
		}

		for (GKInstance childInst : childInstances) {
			System.out.println("\t" + childInst);
			Set<String> attributesContainingReferrals = new HashSet<>();
			Map<String,Set<GKInstance>> referralInstancesByAttribute = new HashMap<>();
			for (Object referralAttributeObj : childInst.getSchemClass().getReferers()) {
					GKSchemaAttribute referralAttribute = (GKSchemaAttribute) referralAttributeObj;
					Collection<GKInstance> childInstReferrals = childInst.getReferers(referralAttribute);
					if (childInstReferrals.size() > 0) {
						attributesContainingReferrals.add(referralAttribute.getName());
						referralInstancesByAttribute.put(referralAttribute.getName(), (Set<GKInstance>) childInstReferrals);
					}
			}

			// Now we have a Map of the childs referrals (referralInstancesByAttribute), as well as a Set of the instance types that contained referrals (attributesContainingReferrals).
			// If the validAttributesForInstance Set is equal to attributesContainingReferrals Set, that means this Instance 'might' be an orphan.
			// If not, the Instance is not an orphan and will not be removed.

			if (attributesContainingReferrals.equals(validSubunitAttributesForInstance)) {
				attributesContainingReferrals.remove("inferredTo");

				boolean childHasAdditionalReferrals = false;
				for (String childReferralAttribute : attributesContainingReferrals) {
					//TODO: Logic here needs a review
					if (referralInstancesByAttribute.get(childReferralAttribute).size() == 1 && referralInstancesByAttribute.get(childReferralAttribute).contains(orphanEntityInst)) {

					} else {
						// Additional referrals aside from original instance in hasMember/hasCandidate/hasComponent/repeatedUnit were found.
						// This logic is required due to the fact EntitySets can have referrals from the 'hasCandidate' and 'hasMember' attributes.
						// This means that it could have a single 'hasMember' referral that points to the orphanEntityInst, while still
						// having additional referrals in the 'hasCandidate' attribute that would prevent deletion, or vice versa.
						childHasAdditionalReferrals = true;
						System.out.println("Child instance has additional child referrals -- aborting deletion attempt of instance");
					}
				}
				if (!childHasAdditionalReferrals) {
					// The childInst passes all 'referral' checks (and can be deleted) -- now we need to check its children
					for (String subunitAttribute : subunitAttributes) {
						if (childInst.getSchemClass().isValidAttribute(subunitAttribute)) {
							for (GKInstance subunitInst : (Collection<GKInstance>) childInst.getAttributeValuesList(subunitAttribute)) {
								SchemaClass subunitSchemClass = subunitInst.getSchemClass();
								if (subunitSchemClass.isa("EntitySet") || subunitSchemClass.isa("Complex") ||  subunitSchemClass.isa("Polymer")) {
									// The child Instance has children of its own. We want to make sure we're not creating orphans as we're removing orphans, so we repeat the process.
									recursionCount++;
									recurseChildInstancesAndDelete(subunitInst, recursionCount);
								} else {
									System.out.println("\t\t" + subunitInst);
									// This subunit instance doesn't have children, but needs to be checked for referrals. If it's only referrals are
									// 'inferredTo' and its (soon to be deleted) parent, it can also be deleted.
									Set<String> validChildSubunitAttributesForInstance = new HashSet<>(Arrays.asList("inferredTo", subunitAttribute));
									Set<String> subunitAttributesContainingReferrals = new HashSet<>();
									Map<String,Set<GKInstance>> subunitReferralInstancesByAttribute = new HashMap<>();
									for (Object subunitReferralAttributeObj : subunitInst.getSchemClass().getReferers()) {
										GKSchemaAttribute subunitReferralAttribute = (GKSchemaAttribute) subunitReferralAttributeObj;
										Collection<GKInstance> subunitInstReferrals = subunitInst.getReferers(subunitReferralAttribute);
										if (subunitInstReferrals.size() > 0) {
											subunitAttributesContainingReferrals.add(subunitReferralAttribute.getName());
											subunitReferralInstancesByAttribute.put(subunitReferralAttribute.getName(), (Set<GKInstance>) subunitInstReferrals);
										}
									}

									if (subunitAttributesContainingReferrals.equals(validChildSubunitAttributesForInstance)) {
										subunitAttributesContainingReferrals.remove("inferredTo");
										boolean subunitHasAdditionalReferrals = false;
										for (String subunitReferralAttribute : subunitAttributesContainingReferrals) {
											if (subunitReferralInstancesByAttribute.get(subunitReferralAttribute).size() == 1 && subunitReferralInstancesByAttribute.get(subunitReferralAttribute).contains(childInst)) {

											} else {
												subunitHasAdditionalReferrals = true;
											}
										}
										if (!subunitHasAdditionalReferrals) {
											// TODO: Delete instance
											System.out.println("\t\tDeleting subunit instance " + subunitInst.getDBID());
										} else {
											System.out.println("\t\t " + subunitInst.getDBID() + " has additional referrals -- aborting deletion attempt of subunit instance");
										}
									} else {
										System.out.println("\t\t " + subunitInst.getDBID() + " has additional referrals -- aborting deletion attempt of subunit instance");
									}
								}
							}
						}
					}
					//TODO: Delete instance
					System.out.println("\tDeleting child instance " + childInst.getDBID());
				} else {
					System.out.println("\t " + childInst.getDBID() + " has additional referrals -- aborting deletion attempt of child instance");
				}

			} else {
				System.out.println("\t " + childInst.getDBID() + " has additional referrals -- aborting deletion attempt of child instance");
			}
		}
	}
}

