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

/**
 *
 */
public class PurgeWait {

    private final String trackingId;

    private final Connection connection;

    /**
     * Create the purge request
     *
     * @param connection  DB connection
     * @param trackingId  The tracking-id to wait for to be removed from the queue
     */
    public PurgeWait(Connection connection, String trackingId) {
        this.connection = connection;
        this.trackingId = trackingId;
    }

    private static final String QUEUE_SIZE = "SELECT COUNT (*) FROM queue WHERE trackingid=?";

    public void waitForQueue() throws SQLException, IOException {
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
                return;
            } else {
                System.out.println("Press enter to wait again.");
                System.in.read();
            }
        }
    }
}
