package org.reactome.release.common;

import java.sql.SQLException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.persistence.MySQLAdaptor;

/**
 * This class provides the framework for a release step in Reactome's data-release-pipeline
 * @author sshorser
 */
public abstract class ReleaseStep
{

	/**
	 * General logger for this release step
	 */
	protected static final Logger logger = LogManager.getLogger();

	/**
	 * Flag to determine if this step is being run live or as a test (false and true, respectively)
	 */
	protected boolean testMode;

	/**
	 * Parses a Properties object to obtain database connection information and return a database adaptor as a
	 * MySQLAdaptor object.  Properties for db.host, db.user, db.password, db.name, and optionally db.port are
	 * expected.
	 *
	 * @param props Properties object containing database connection properties
	 * @return Database adaptor as a MySQLAdaptor object
	 * @throws SQLException Thrown if unable to create a MySQLAdaptor object to return
	 */
	protected static MySQLAdaptor getMySQLAdaptorFromProperties(Properties props) throws SQLException
	{

		String dbHost = props.getProperty("db.host", "localhost");
		String dbUser = props.getProperty("db.user");
		String dbPassword = props.getProperty("db.password");
		String dbName = props.getProperty("db.name");
		int dbPort = new Integer(props.getProperty("db.port", "3306"));

		MySQLAdaptor adaptor = new MySQLAdaptor(dbHost, dbName, dbUser, dbPassword, dbPort);
		return adaptor;
	}

	/**
	 * Parses a Properties object to extract and set the value of the testMode flag, from a "testMode" property,
	 * for this release step.  If no "testMode" value is present, the testMode flag is set to true by default.
	 *
	 * @param props Properties object containing the testMode value
	 */
	protected void loadTestModeFromProperties(Properties props)
	{
		this.testMode = new Boolean(props.getProperty("testMode", "true"));
		if (!testMode)
		{
			logger.info("Test mode is OFF - database will be updated!");
		}
		else
		{
			logger.info("Test mode is ON - no database changes will be made.");
		}
	}

	/**
	 * Executes the logic to carry out this release step.
	 *
	 * @param props Properties object containing any required values for running the release step
	 * @throws Exception Thrown if the execution of the release step fails in an unrecoverable manner
	 */
	abstract public void executeStep(Properties props) throws Exception;
}
