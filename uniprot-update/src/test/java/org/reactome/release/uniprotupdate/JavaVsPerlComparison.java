package org.reactome.release.uniprotupdate;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.MySQLAdaptor.AttributeQueryRequest;
import org.reactome.util.compare.DBObjectComparer;

public class JavaVsPerlComparison {

	public static void main(String[] args) throws Exception {
		MySQLAdaptor javaDBA = new MySQLAdaptor(
			"localhost",
			"gk_central_pre_R66.dump.sql",
			"root",
			"root",
			3308
		);
		MySQLAdaptor perlDBA = new MySQLAdaptor(
			"localhost",
			"gk_central_after_uniprot_update.dump.sql",
			"root",
			"root",
			3307
		);

//		Set<GKInstance> javaRefDNASequences = (Set<GKInstance>) javaDBA.fetchInstancesByClass(
//			ReactomeJavaConstants.ReferenceDNASequence
//		);
//		System.out.println(javaRefDNASequences.size() + " Reference DNA Sequences");
//		Set<GKInstance> javaRefGeneProd = (Set<GKInstance>) javaDBA.fetchInstancesByClass(
//			ReactomeJavaConstants.ReferenceGeneProduct
//		);
//		System.out.println(javaRefGeneProd.size() + " Reference Gene Products");
//		Set<GKInstance> javaRefIsoform = (Set<GKInstance>) javaDBA.fetchInstancesByClass(
//			ReactomeJavaConstants.ReferenceIsoform
//		);
//		System.out.println(javaRefIsoform.size() + " Reference Isoforms");

		long startTime = System.currentTimeMillis();

		List<String> classNames =
			Arrays.asList(ReactomeJavaConstants.ReferenceGeneProduct, ReactomeJavaConstants.ReferenceIsoform);
		for (String className : classNames) {
			int sameCount = 0;
			int diffCount = 0;
			int totalComparisonsCount = 0;
			int inJavaNotInPerl = 0;
			@SuppressWarnings("unchecked")
			Set<GKInstance> javaReferenceEntities = (Set<GKInstance>) javaDBA.fetchInstancesByClass(className);
			@SuppressWarnings("unchecked")
			Set<GKInstance> perlReferenceEntities = (Set<GKInstance>) perlDBA.fetchInstancesByClass(className);
			System.out.println(
				"*****\n"+javaReferenceEntities.size() + " " + className + " instances in Java-modified database."
			);
			System.out.println(
				perlReferenceEntities.size() + " " + className + " instances in Perl-modified database.\n"
			);

			for (GKInstance refEnt : javaReferenceEntities.stream().filter(
				instance -> instance.getSchemClass().getName().equals(className)
			).collect(Collectors.toList())) {
				String identifier = (String) refEnt.getAttributeValue(ReactomeJavaConstants.identifier);
				GKInstance refDB = (GKInstance) refEnt.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
				if (identifier != null && refDB != null) {
					AttributeQueryRequest identifierAqRequest = perlDBA.new AttributeQueryRequest(
						className, ReactomeJavaConstants.identifier, "=", identifier
					);
					AttributeQueryRequest refDBAqRequest = perlDBA.new AttributeQueryRequest(
						className, ReactomeJavaConstants.referenceDatabase, "=", refDB
					);

//					Set<GKInstance> perlInstances = (Set<GKInstance>) perlDBA.fetchInstanceByAttribute(
//						ReactomeJavaConstants.ReferenceDNASequence,
//						ReactomeJavaConstants.DB_ID,
//						"=",
//						refDNASeq.getDBID()
//					);
					@SuppressWarnings("unchecked")
					Set<GKInstance> perlInstances = perlDBA.fetchInstance(
						Arrays.asList(identifierAqRequest, refDBAqRequest)
					);
					if (perlInstances != null && perlInstances.size() > 0) {
						if (perlInstances.size() > 1) {
							System.out.println(
								perlInstances.size() + " Perl instances match identifier/refdb: " + identifier + "/" +
								refDB.toString()
							);
						}
						for (GKInstance perlInst : perlInstances.stream().filter(
							instance -> instance.getSchemClass().getName().equals(className)
						).collect(Collectors.toList())) {
							StringBuilder sb = new StringBuilder();
							totalComparisonsCount++;
							int diffs = DBObjectComparer.compareInstances(refEnt, perlInst, sb, false);
							if (diffs > 0) {
								diffCount++;
								System.out.println(
									diffs + " differences for \"" + refEnt.toString() +
									"\" vs Perl instance \""+perlInst.toString()+"\": \n"+sb.toString()
								);
							} else {
								sameCount++;
							}
						}
					} else {
						System.out.println("Identifier " + identifier + " is not in Perl-modified database.");
					}
				} else {
					System.out.println(className + " instance \""+refEnt.toString()+"\" has no identifier!");
				}

				if (totalComparisonsCount > 0 && totalComparisonsCount % 1000 == 0) {
					long endTime = System.currentTimeMillis();
					System.out.println(totalComparisonsCount + " comparisons have been performed in " +
						Duration.ofMillis(endTime - startTime).toString() );
				}
			}
			System.out.println("Number of comparisons performed: "+totalComparisonsCount);
			System.out.println("Number of instances that are different: "+diffCount);
			System.out.println("Number of instances that are the same: "+sameCount+"\n");
		}
	}
}
