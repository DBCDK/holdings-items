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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import dk.dbc.commons.jdbc.util.DatabaseConnectionDetails;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.DatabaseMigrator;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.openagency.client.OpenAgencyException;
import dk.dbc.openagency.client.OpenAgencyServiceFromURL;
import dk.dbc.openagency.client.PickupAgency;
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
        try {
            Arguments commandLine = new Arguments( args );
            if (commandLine.hasVerbose()) {
                System.out.println("Setting  level to debug");
                LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
                context.getLogger("dk.dbc.holdingsitems.purge").setLevel(Level.DEBUG);
            }

            int agencyId = commandLine.getAgencyId();
            log.info( "Agency ID: {}", agencyId);

            String agencyUrl = commandLine.getOpenAgencyUrl();

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

            String db = commandLine.getDatabase();
            log.info( "DB: {}", db);

            String queue = commandLine.getQueue();
            log.info( "Queue: {}", queue);

            int commitEvery = commandLine.getCommit().orElse( 0 );
            log.info( "Commit every {} records per thread", commitEvery);

            boolean dryRun = commandLine.hasDryRun();
            if ( dryRun ) {
                log.info( "Dry run. Data will be rolled back" );
            }

            // Create database connection and process
            DataSource dataSource = getDataSource(db);
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, "PurgeTool");

                Purge execute = new Purge(connection, dao, queue, agencyName, agencyId, commitEvery, dryRun);
                execute.process();
            } catch (SQLException | HoldingsItemsException ex) {
                log.error( "Exception", ex );
                System.exit( 1 );
            }
        } catch ( ExitException ex ) {
            System.exit( ex.getStatus() );
        }
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

}
