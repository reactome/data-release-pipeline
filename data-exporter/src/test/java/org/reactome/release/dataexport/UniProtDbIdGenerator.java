package org.reactome.release.dataexport;

public class UniProtDbIdGenerator {
	private static long uniProtDbId = 1L;

	public static long getNextUniProtDBID() {
		return uniProtDbId++;
	}
}
