package org.reactome.release.dataexport;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Since some objects are cached and depend upon having distinct database identifiers, this class will produce unique
 * database identifiers to ensure there are no clashes in the database identifers used for objects in test classes and
 * methods
 *
 * @author jweiser
 */
public class DbIdGenerator {
	private static AtomicLong dbId = new AtomicLong(1L);

	public static long getNextDBID() {
		return dbId.getAndIncrement();
	}
}
