package org.reactome.release.orthoStableIdentifierHistory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.persistence.MySQLAdaptor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Properties;

public class Main {

    private static final Logger logger = LogManager.getLogger();
    private static final String RESOURCES_DIR = Paths.get("src", "main", "resources").toString();

    public static void main(String[] args) throws IOException, SQLException {

        String pathToConfig = args.length > 0 ? args[0] : Paths.get(RESOURCES_DIR ,"config.properties").toString();

        Properties props = new Properties();
        props.load(new FileInputStream(pathToConfig));

        //Set up DB adaptor
        String username = props.getProperty("release.database.user");
        String password = props.getProperty("release.database.password");
        String host = props.getProperty("release.database.host");
        int port = Integer.valueOf(props.getProperty("release.database.port"));

        String releaseCurrent = props.getProperty("release_current.name");

        MySQLAdaptor dbAdaptor = new MySQLAdaptor(host, releaseCurrent, username, password, port);

        logger.info("Executing post-step checks");
        PostStepChecks.performStableIdentifierHistoryQA(dbAdaptor);
    }
}
