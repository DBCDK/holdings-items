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
import dk.dbc.holdingsitems.jpa.JpaByDbUrl;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 * Command line to purge an entire agency from holdings-items database
 */
public class PurgeMain {

    private static final Logger log = LoggerFactory.getLogger(PurgeMain.class);

    public static void main(String[] args) throws //OpenAgencyException,
                                                  IOException {
        try {
            Arguments commandLine = new Arguments(args);
            if (commandLine.hasVerbose()) {
                System.out.println("Setting  level to debug");
                LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
                context.getLogger("dk.dbc.holdingsitems.purge").setLevel(Level.DEBUG);
            }

            int agencyId = commandLine.getAgencyId();
            log.info("Agency ID: {}", agencyId);

            String vipCoreUrl = commandLine.getVipCoreUrl();

            String agencyName = new VipCoreConnector(vipCoreUrl).lookupAgencyName(agencyId);
            log.info("VipCore agency {}: Name '{}'", agencyId, agencyName);

            String db = commandLine.getDatabase();
            log.info("DB: {}", db);

            List<String> queues = Arrays.asList(commandLine.getQueue().split(","))
                    .stream()
                    .filter(s -> !s.isEmpty())
                    .collect(toList());
            if(queues.isEmpty()) {
                throw new IllegalArgumentException("No queues are defined");
            }
            log.info("Queues: {}", queues);

            boolean removeFirstAcquisitionDate = commandLine.hasRemoveFirstAcquisitionDate();
            if (removeFirstAcquisitionDate) {
                log.info("Will remove ALL traces of the agency");
            }

            boolean dryRun = commandLine.hasDryRun();
            if (dryRun) {
                log.info("Dry run. Data will be rolled back");
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
            String trackingId = String.format("PurgeTool-%s-%s", agencyId, timestamp);
            log.info("trackingId: {}", trackingId);

            // Create database connection and process
            DataSource dataSource = getDataSource(db);
            JpaByDbUrl jpa = new JpaByDbUrl(db, "purge-tool");

            try {
                jpa.run(em -> {
                    HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
                    PurgeReport purgeReport = new PurgeReport(dao, agencyId);
                    purgeReport.statusReport();
                });

                jpa.run(em -> {
                    HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, trackingId);
                    Purge purge = new Purge(em, dao, queues, agencyName, agencyId, removeFirstAcquisitionDate, dryRun);
                    purge.process();
                });

                try (Connection connection = dataSource.getConnection()) {
                    PurgeWait purgeWait = new PurgeWait(connection, trackingId);
                    purgeWait.waitForQueue();
                }

                jpa.run(em -> {
                    HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
                    PurgeReport purgeReport = new PurgeReport(dao, agencyId);
                    purgeReport.statusReport();
                });

            } catch (SQLException | RuntimeException ex) {
                log.error("Exception", ex);
                System.exit(1);
            }
        } catch (ExitException ex) {
            System.exit(ex.getStatus());
        }
    }

    /**
     * Setup the database connection
     *
     * @param url URL for holdings-items database
     * @return The data source
     */
    private static DataSource getDataSource(String url) {
        log.info("Create Data Source for {}", url);
        DatabaseConnectionDetails details = DatabaseConnectionDetails.parse(url);
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(details.getConnectString());
        dataSource.setUser(details.getCredentials().getProperty("user"));
        dataSource.setPassword(details.getCredentials().getProperty("password"));

        return dataSource;
    }

}
