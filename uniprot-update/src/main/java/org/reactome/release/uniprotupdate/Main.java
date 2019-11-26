package org.reactome.release.uniprotupdate;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author sshorser
 *
 */
public class Main {

	public static void main(String[] args) throws IOException {
		String fileName = args.length > 0 ? args[0] : "uniprot-update.properties";

		Properties props = new Properties();
		props.load(new FileInputStream(fileName));

		UniprotUpdateStep step = new UniprotUpdateStep();
		try {
			step.executeStep(props);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
