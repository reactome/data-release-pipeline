package org.reactome.database;

import java.io.FileInputStream;
import java.util.*;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.SchemaAttribute;

public class PhysicalEntityUpdater {

	public static void main(String[] args) throws Exception {

		String pathToConfig = "src/main/resources/config.properties";
		Properties props = new Properties();
		props.load(new FileInputStream(pathToConfig));
		
		String username = props.getProperty("username");
		String password = props.getProperty("password");
		String host = props.getProperty("host");
		String database = props.getProperty("database");
		int port = Integer.valueOf(props.getProperty("port"));
		
		MySQLAdaptor dba = new MySQLAdaptor(host, database, username, password, port);
		Collection<GKInstance> physicalEntityInstances = dba.fetchInstancesByClass("PhysicalEntity");
		Map<String,Map<String,Map<String,Integer>>> referralsMap = new HashMap<>();
		
		// Iterate through each PE
		int orphanCount = 0;
//		Set<String> singleReferralAttributes = new HashSet<>();
//		Set<String> allReferralAttributes = new HashSet<>();
		List<Long> orphanPEs = new ArrayList<>();
		for (GKInstance physicalEntityInst : physicalEntityInstances) {
//			orphanCount++;

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
				orphanPEs.add(physicalEntityInst.getDBID());
			}
		}


	}
}

