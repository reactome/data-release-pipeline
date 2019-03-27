package org.reactome.orthoinference;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

	private static final Logger logger = LogManager.getLogger();
	
	public static void main(String[] args) throws Exception {
		
		String 	pathToConfig = "";
		String  refSpeciesCode = "";
		String	projSpeciesCode = "";
		int 	threshold = 0;

		if (args.length == 4) {
			pathToConfig	= args[0];
			refSpeciesCode	= args[1];
			projSpeciesCode	= args[2];
			threshold		= Integer.parseInt(args[3]);
		} else {
			logger.fatal("args - all required\n" +
					"configPath: path to config file\n" +
					"refSpecies: Reference species (4-digit abbv)\n" +
					"projSpecies: Projected species (4-digit abbv); may contain multiple species, space-delimited\n" +
					"threshold: Minimum percentage of orthologous catalysts required to project event\n");
			System.exit(0);
		}
		
		Properties props = new Properties();
		props.load(new FileInputStream(pathToConfig));
        EventsInferrer.inferEvents(props, refSpeciesCode, projSpeciesCode, threshold);
	}

}
