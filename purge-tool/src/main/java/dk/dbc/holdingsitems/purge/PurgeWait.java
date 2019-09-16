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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class PurgeWait {

    private static final Logger log = LoggerFactory.getLogger(PurgeWait.class);
    private final String agencyName;
    private final int agencyId;
    private final String trackingId;

    private final boolean dryRun;

    private final Connection connection;

    /**
     * Create the purge request
     *
     * @param connection  DB connection
     * @param agencyName  The name of the agency to verify against
     * @param agencyId    The agency ID to be processed
     * @param trackingId  The tracking-id to wait for to be removed from the queue
     * @param dryRun      Optionally check but does not commit anything
     */
    public PurgeWait(Connection connection, String agencyName, int agencyId, String trackingId, boolean dryRun) {
        this.connection = connection;
        this.agencyName = agencyName;
        this.agencyId = agencyId;
        this.dryRun = dryRun;
        this.trackingId = trackingId;
    }


    /**
     * Commit or rollback for database
     *
     * @param count Count for log status
     * @throws SQLException
     */
    private void commit(int count) throws SQLException {
        if (dryRun) {
            log.info("Rolled back at {}", count);
            connection.rollback();
        } else {
            log.info("Commit at {}", count);
            connection.commit();
        }
    }

    private static final String QUEUE_SIZE = "SELECT COUNT (*) FROM queue WHERE trackingid=?";
    private static final String PURGE_ITEMS = "DELETE FROM holdingsitemsitem WHERE agencyId=?";
    private static final String PURGE_COLLECTIONS = "DELETE FROM holdingsitemscollection WHERE agencyId=?";

    public  void waitForQueue() throws SQLException, IOException {
        System.out.println("Waiting for queue to be processed");
        int count = 1;
        while (true) {
            try (PreparedStatement stmt = connection.prepareStatement(QUEUE_SIZE)) {
                stmt.setString(1, trackingId);
                try (ResultSet resultSet = stmt.executeQuery()) {
                    if (resultSet.next()) {
                        count = resultSet.getInt(1);
                        System.out.printf("Queue '%s' has %d items%n", trackingId, count);
                    }
                }
            }
            if (count == 0) {
                System.out.printf("Delete items and collections for %d: %s%n", agencyId, agencyName);
                try (PreparedStatement deleteItems = connection.prepareStatement(PURGE_ITEMS) ;
                     PreparedStatement deleteCollections = connection.prepareStatement(PURGE_COLLECTIONS);) {
                    deleteItems.setInt(1, agencyId);
                    int itemCount = deleteItems.executeUpdate();
                    System.out.printf("%d items deleted from tables%n", itemCount);

                    deleteCollections.setInt(1, agencyId);
                    int collectionsCount = deleteCollections.executeUpdate();
                    System.out.printf("%d collections, deleted from tables%n", collectionsCount);
                    commit(collectionsCount);
                }
                return;
            } else {
                System.out.println("Press enter to wait again.");
                System.in.read();
            }
        }
    }
}
