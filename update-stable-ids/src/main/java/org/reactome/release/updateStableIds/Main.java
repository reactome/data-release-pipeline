package org.reactome.release.updateStableIds;

import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.persistence.MySQLAdaptor;

/**
 * This function iterates through all instances, and checks if it has been changed since the previous release.
 * Instances that have been changed have their 'identifierVersion' attribute incremented by 1 to reflect a change. 
 *
 */
public class Main 
{
	private static final Logger logger = LogManager.getLogger();
	
    public static void main( String[] args ) throws Exception
    {
    	logger.info("Beginning UpdateStableIds step...");
       
    	String pathToConfig = "";
    	if (args.length > 0) {
    		pathToConfig = args[0];
    	} else {
    		pathToConfig = "src/main/resources/config.properties";
    	}
       
	System.out.println(pathToConfig);
       //Sets up the various DB Adaptors needed. This includes the current and previous test_slice versions on the release server, 
       //as well as gk_central on the curation server.
       Properties props = new Properties();
       props.load(new FileInputStream(pathToConfig));
       int port = Integer.valueOf(props.getProperty("release.database.port"));
       int releaseNumber = Integer.valueOf(props.getProperty("releaseNumber"));
       int prevReleaseNumber = releaseNumber - 1;

       MySQLAdaptor dbaSlice = createDatabaseAdaptor(props, "release.database.user", "release.database.password", "release.database.host","slice_current.name", "release.database.port");
       MySQLAdaptor dbaPrevSlice = createDatabaseAdaptor(props, "release.database.user", "release.database.password", "release.database.host","slice_previous.name", "release.database.port");
       MySQLAdaptor dbaGkCentral = createDatabaseAdaptor(props, "curator.database.user", "curator.database.password", "curator.database.host", "curator.database.name", "curator.database.port");
       
       long personId = Long.parseLong(props.getProperty("personId"));
       StableIdentifierUpdater.updateStableIdentifiers(dbaSlice, dbaPrevSlice, dbaGkCentral, personId);
 

       logger.info("Finished UpdateStableIds step");
;    }

    private static MySQLAdaptor createDatabaseAdaptor(Properties props, String userProperty, String passwordProperty, String hostProperty, String databaseNameProperty, String portProperty) throws SQLException {
        String userName = props.getProperty(userProperty);
        String password = props.getProperty(passwordProperty);
        String host = props.getProperty(hostProperty);
        String databaseName = props.getProperty(databaseNameProperty);
        int port = Integer.parseInt(props.getProperty(portProperty));

        logger.info("Creating DB adaptor for " + databaseName + " at " + host + " on port " + port + " for user " + userName);

        return new MySQLAdaptor(host, databaseName, userName, password, port);
    }
}
