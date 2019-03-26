/*
 This file is part of opensearch.
 Copyright © 2013, Dansk Bibliotekscenter a/s,
 Tempovej 7-11, DK-2750 Ballerup, Denmark. CVR: 15149043

 opensearch is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 opensearch is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with opensearch.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.purge;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import dk.dbc.commons.jdbc.util.DatabaseConnectionDetails;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.DatabaseMigrator;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.openagency.client.OpenAgencyException;
import dk.dbc.openagency.client.OpenAgencyServiceFromURL;
import dk.dbc.openagency.client.PickupAgency;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command line to purge an entire agency from holdings-items database
 */
public class PurgeMain {

    private static final Logger log = LoggerFactory.getLogger(PurgeMain.class );

    public static void main( String[] args ) throws OpenAgencyException {
        CommandLine commandLine = new IngestCommandLine();

        try {
            commandLine.parse( args );
            if ( commandLine.hasOption( "debug" ) ) {
                log.info( "Loglevel: DEBUG");
                setLogLevel( "logback-debug.xml" );
            }
            else {
                log.info( "Loglevel: INFO");
                setLogLevel( "logback-info.xml" );
            }
        }
        catch ( IllegalStateException | NumberFormatException ex ) {
            System.err.println( ex.getMessage() );
            System.err.println( commandLine.usage() );
            System.exit( 1 );
            return;
        }
        catch ( JoranException ex ) {
            log.error( "Exception", ex );
            System.exit( 1 );
            return;
        }


        int agencyId = (int) commandLine.getOption( "agencyid" );
        log.info( "Agency ID: {}", agencyId);
       
        String agencyUrl = (String) commandLine.getOption( "openagencyurl" );
        
        log.info( "OpenAgency URL: {}", agencyUrl);
        OpenAgencyServiceFromURL openAgency = OpenAgencyServiceFromURL.builder().build(agencyUrl);
        log.debug( "OpenAgency lookup agency {}", agencyId);
        
        final PickupAgency agency = openAgency.findLibrary().findLibraryByAgency(agencyId);
        String agencyName;
        if (agency == null) {
            log.warn( "OpenAgency agency {}: is unknown", agencyId);
            agencyName = "<unknown>";
        } else {
            agencyName = agency.getAgencyName();
        }
        
        log.info( "Agency agency {}: Name '{}'", agencyId, agencyName);
       
        String db = (String) commandLine.getOption( "db" );
        log.info( "DB: {}", db);
        
        String worker = (String) commandLine.getOption( "worker" );
        log.info( "Worker: {}", worker);
        
        int commitEvery = commandLine.hasOption("commit") ? (Integer) commandLine.getOption( "commit" ) : 0;
        log.info( "Commit every {} records per thread", commitEvery);

        boolean dryRun = commandLine.hasOption( "dry-run" );
        if ( dryRun ) {
            log.info( "Dry run. Data will be rolled back" );
        }
        
        // Create database connection and process
        DataSource dataSource = getDataSource(db);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, "PurgeTool");

            Purge execute = new Purge(connection, dao, worker, agencyName, agencyId, commitEvery, dryRun);
            execute.process();
        } catch (SQLException | HoldingsItemsException ex) {
            log.error( "Exception", ex );
            System.exit( 1 );
        }
    }

    /**
     * Create log levels
     * @param file log level configuration
     * @throws JoranException In case log level file has errors
     */
    private static void setLogLevel( String file ) throws JoranException {
        LoggerContext context = ( LoggerContext ) LoggerFactory.getILoggerFactory();
        context.reset();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        InputStream stream = contextClassLoader.getResourceAsStream( file );
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext( context );
        configurator.doConfigure( stream ); // loads logback file
        StatusPrinter.printInCaseOfErrorsOrWarnings( context ); // Internal status data is printed in case of warnings or errors.
    }
    
    /**
     * Setup the database connection
     * @param url URL for holdings-items database
     * @return The data source
     */
    private static DataSource getDataSource(String url) {
        log.info( "Create Data Source for {}", url );
        DatabaseConnectionDetails details = DatabaseConnectionDetails.parse(url);
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(details.getConnectString());
        dataSource.setUser(details.getCredentials().getProperty("user"));
        dataSource.setPassword(details.getCredentials().getProperty("password"));
        
        DatabaseMigrator.migrate(dataSource);
        
        return dataSource;
    }
    

    private static class IngestCommandLine extends CommandLine {
        @Override
        void setOptions() {
            addOption( "agencyid", "Agency ID to purge for", true, false, integer, null );
            addOption( "openagencyurl", "OpenAgency URL to connect to. E.g. http://openagency.addi.dk/<version>/", true, false, string, null );
            addOption( "db", "connectstring for database. E.g jdbc:postgresql://user:password@host:port/database", true, false, string, null );
            addOption( "worker", "Worker to be enqueued to", true, false, string, null );
            addOption( "debug", "turn on debug logging", false, false, null, yes );
            addOption( "commit", "commit every n times. Default is 0 meaning commit only at end", false, false, integer, int0 );
            addOption( "dry-run", "Do not commit anything", false, false, null, no );
        }

        @Override
        String usageCommandLine() {
            return "prog [ options ]";
        }
    }

}
