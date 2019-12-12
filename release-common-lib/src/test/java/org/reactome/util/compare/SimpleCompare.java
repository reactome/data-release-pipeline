package org.reactome.util.compare;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;

/**
 * Prints the differences between the version of instance obtained from two databases
 */
public class SimpleCompare {
	private static final long INSTANCE_DB_ID_TO_COMPARE = 5263264;

	public static void main(String[] args) throws Exception {
		MySQLAdaptor database1 = new MySQLAdaptor("localhost", "perl_gk_central", "root", "root",
			3306);
		MySQLAdaptor database2 = new MySQLAdaptor("localhost", "java_gk_central", "root", "root",
			3306);

		GKInstance instance1 = database1.fetchInstance(INSTANCE_DB_ID_TO_COMPARE);
		GKInstance instance2 = database2.fetchInstance(INSTANCE_DB_ID_TO_COMPARE);

		StringBuilder sb = new StringBuilder();
		int i = DBObjectComparer.compareInstances(instance1, instance2, sb, 0, true);
		System.out.println(
			"\n***\n" +
			"For instance " + instance1.toString() + " there are " + i + " differences:\n" +
			sb.toString()
		);
	}
}
