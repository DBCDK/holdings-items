/*
 * Copyright (C) 2014-2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items
 *
 * holdings-items is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems;

import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class HoldingsItemsDAOIT extends DbBase {

    @Test
    public void testValidateSchema() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("UPDATE version SET warning=NULL");
                    stmt.execute("UPDATE version SET warning='TEST VALUE' WHERE version = (SELECT MAX(version) FROM version)");
                }

                AtomicReference<String> ref = new AtomicReference<>();

                HoldingsItemsDAO dao = new HoldingsItemsDAO(connection, "") {
                    @Override
                    void logValidationError(String warning) {
                        System.out.println("warning = " + warning);
                        ref.set(warning);
                    }
                };
                dao.validateConnection();
                String warning = ref.get();
                if (warning == null || !warning.contains("TEST VALUE")) {
                    fail("Expected Logged Warning");
                }

            } finally {
                connection.rollback();
            }
        }
    }

    @Test
    public void testReadWriteRecord() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, "FOO");
            RecordCollection col = dao.getRecordCollection("1234567890", 870970, "1234567890");
            Record record = col.findRecord("A1");
            assertTrue("Newly created record is modified", record.isModified());
            col.setIssueText("text 1");
            col.setExpectedDelivery(dateString("2015-01-01"));
            record.setBranch("B1");
            record.setDepartment("D1");
            record.setLocation("L1");
            record.setSubLocation("S1");
            record.setCirculationRule("ANY");
            record.setAccessionDate(dateString("2014-06-16"));
            record.setStatus(Record.OnOrder);
            col.save();

            RecordCollection col2 = dao.getRecordCollection("1234567890", 870970, "1234567890");
            Record record2 = col2.findRecord("A1");
            assertFalse("Record from db is not modified", record2.isModified());
            assertEquals(col2.getExpectedDelivery().getTime(), col.getExpectedDelivery().getTime());
        }
    }

    @Test
    public void testQueue() throws HoldingsItemsException, SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection);
            dao.enqueueOld("987654321", 870970, "solrIndex1");
            connection.commit();
            List<QueueJob> queue = queue();
            System.out.println("queue = " + queue);
            assertEquals("Sizeof queue is 1", 1, queue.size());
            assertEquals("Queue Entry 1 starts with: ", "solrIndex1", queue.get(0).getWorker());
            Instant queued = queue.get(0).getQueued();
            long diff = Math.abs(queued.toEpochMilli() - Instant.now().toEpochMilli());
            System.out.println("diff = " + diff);
            assertTrue("Not too long ago it has been queued", diff < 500);
        }
    }

    @Test
    public void testGetAgenciesThatHasHoldingsFor() throws HoldingsItemsException, SQLException {
        try (Connection connection = dataSource.getConnection()) {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, "FOO");
            connection.setAutoCommit(false);

            RecordCollection col = dao.getRecordCollection("1234567890", 870970, "1234567890");
            Record record = col.findRecord("A1");
            assertTrue("Newly created record is modified", record.isModified());
            col.setIssueText("text 1");
            col.setExpectedDelivery(dateString("2015-01-01"));
            record.setBranch("B1");
            record.setDepartment("D1");
            record.setLocation("L1");
            record.setSubLocation("S1");
            record.setCirculationRule("ANY");
            record.setAccessionDate(dateString("2014-06-16"));
            record.setStatus(Record.OnOrder);
            col.save();

            col = dao.getRecordCollection("1234567890", 123456, "1234567890");
            record = col.findRecord("A1");
            assertTrue("Newly created record is modified", record.isModified());
            col.setIssueText("text 1");
            col.setExpectedDelivery(dateString("2015-01-01"));
            record.setBranch("B1");
            record.setDepartment("D1");
            record.setLocation("L1");
            record.setSubLocation("S1");
            record.setCirculationRule("ANY");
            record.setAccessionDate(dateString("2014-06-16"));
            record.setStatus(Record.Decommissioned);
            col.save();

            col = dao.getRecordCollection("1234567890", 700000, "1234567890");
            record = col.findRecord("A1");
            assertTrue("Newly created record is modified", record.isModified());
            col.setIssueText("text 1");
            col.setExpectedDelivery(dateString("2015-01-01"));
            record.setBranch("B1");
            record.setDepartment("D1");
            record.setLocation("L1");
            record.setSubLocation("S1");
            record.setCirculationRule("ANY");
            record.setAccessionDate(dateString("2014-06-16"));
            record.setStatus(Record.OnOrder);
            record = col.findRecord("A2");
            assertTrue("Newly created record is modified", record.isModified());
            col.setIssueText("text 2");
            col.setExpectedDelivery(dateString("2015-01-01"));
            record.setBranch("B2");
            record.setDepartment("D2");
            record.setLocation("L2");
            record.setSubLocation("S2");
            record.setCirculationRule("ANY");
            record.setAccessionDate(dateString("2014-06-16"));
            record.setStatus(Record.OnOrder);
            col.save();

            connection.commit();

            Set<Integer> agenciesThatHasHoldingsFor = dao.getAgenciesThatHasHoldingsFor("1234567890");
            assertEquals("Size of response", 2, agenciesThatHasHoldingsFor.size());
            assertTrue("Has 1", agenciesThatHasHoldingsFor.contains(700000));
            assertTrue("Has 2", agenciesThatHasHoldingsFor.contains(870970));
        }
    }

    @Test
    public void testStatusFor() throws Exception {
        try (Connection connection = dataSource.getConnection()) {

            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, "FOO");
            connection.setAutoCommit(false);
            RecordCollection c;
            Record r;
            c = dao.getRecordCollection("12345678", 654321, "a");
            r = c.findRecord("1");
            r.setAccessionDate(new Date());
            r.setStatus("OnLoan");
            r = c.findRecord("2");
            r.setAccessionDate(new Date());
            r.setStatus("OnShelf");
            dao.saveRecordCollection(c, Timestamp.from(Instant.now()));
            c = dao.getRecordCollection("12345678", 654321, "b");
            r = c.findRecord("3");
            r.setAccessionDate(new Date());
            r.setStatus("OnLoan");
            r = c.findRecord("4");
            r.setAccessionDate(new Date());
            r.setStatus("Online");
            dao.saveRecordCollection(c, Timestamp.from(Instant.now()));

            Map<String, Integer> status = dao.getStatusFor("12345678", 654321);

            System.out.println("status = " + status);
            assertEquals(1, (int) status.get("OnShelf"));
            assertEquals(1, (int) status.get("Online"));
            assertEquals(2, (int) status.get("OnLoan"));
            assertEquals(3, status.size());

            connection.commit();
        }
    }

    @Test
    public void testEnqueue() throws Exception {
        System.out.println("testEnqueue");
        try (Connection connection = dataSource.getConnection()) {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, "FOO");
            connection.setAutoCommit(false);
            dao.enqueue("12345678", 888888, "{}", "worker");
            dao.enqueue("87654321", 888888, "{}", "worker", 1000);
            connection.commit();
        }
        try (Connection connection = dataSource.getConnection() ;
             Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("SELECT bibliographicRecordId, agencyId, trackingId FROM queue")) {
            HashSet<String> results = new HashSet<>();
            while (resultSet.next()) {
                int i = 0;
                String biblId = resultSet.getString(++i);
                int agencyId = resultSet.getInt(++i);
                String tracking = resultSet.getString(++i);
                results.add(biblId + "|" + agencyId + "|" + tracking);
            }
            assertEquals(2, results.size());
            assertTrue(results.contains("12345678|888888|FOO"));
            assertTrue(results.contains("87654321|888888|FOO"));
        }
    }

//  _   _      _                   _____                 _   _
// | | | | ___| |_ __   ___ _ __  |  ___|   _ _ __   ___| |_(_) ___  _ __  ___
// | |_| |/ _ \ | '_ \ / _ \ '__| | |_ | | | | '_ \ / __| __| |/ _ \| '_ \/ __|
// |  _  |  __/ | |_) |  __/ |    |  _|| |_| | | | | (__| |_| | (_) | | | \__ \
// |_| |_|\___|_| .__/ \___|_|    |_|   \__,_|_| |_|\___|\__|_|\___/|_| |_|___/
//              |_|
    private static final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("YYYY-MM-DD");

    @SuppressWarnings({"deprecation"})
    List<QueueJob> queue() throws SQLException {
        ArrayList<QueueJob> ret = new ArrayList<>();
        try (Connection connection = dataSource.getConnection() ;
             PreparedStatement stmt = connection.prepareStatement("SELECT " + QueueJob.SQL_COLUMNS + " FROM q") ;
             ResultSet resultSet = stmt.executeQuery()) {
            while (resultSet.next()) {
                ret.add(new QueueJob(resultSet));
            }
        }
        return ret;
    }

    int diags() throws SQLException {
        try (Connection connection = dataSource.getConnection() ;
             PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM diag") ;
             ResultSet resultSet = stmt.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return -1;
    }

    private static Date dateString(String s) {
        try {
            return DATE_PARSER.parse(s);
        } catch (ParseException ex) {
            return new Date(0);
        }
    }

}
