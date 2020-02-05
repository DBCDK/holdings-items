package dk.dbc.holdingsitems.content;

import dk.dbc.commons.testutils.postgres.connection.PostgresITDataSource;
import dk.dbc.holdingsitems.DatabaseMigrator;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.Record;
import dk.dbc.holdingsitems.RecordCollection;
import dk.dbc.holdingsitems.content.response.ContentServiceItemResponse;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ContentIT {
    private static final Logger log = LoggerFactory.getLogger(ContentIT.class);

    private PostgresITDataSource pg;
    private DataSource dataSource;
    private ContentResource contentResource;

    @Before
    public void setup() throws Exception {
        pg = new PostgresITDataSource("holdingsitems");
        dataSource = pg.getDataSource();
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DROP SCHEMA public CASCADE");
            stmt.executeUpdate("CREATE SCHEMA public");
        }
        DatabaseMigrator.migrate(dataSource);
        contentResource = new ContentResource();
        contentResource.dataSource = dataSource;
    }

    @Test(timeout = 30_000L)
    public void testGetItemEntity() throws Exception {
        System.out.println("Test getItemEntity endpoint...");
        try (Connection connection = dataSource.getConnection()) {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, "track1", true);
            RecordCollection issue1 = dao.getRecordCollection("12345678", 654321, "Issue#1");
            issue1.setIssueText("#1");
            issue1.setNote("");
            issue1.setReadyForLoan(1);
            record(issue1, "1234", "OnShelf");
            record(issue1, "2345", "OnLoan");
            issue1.save(Timestamp.from(Instant.now()));

            System.out.println("Calling service...");
            Response response = contentResource.getItemEntity(654321, "1234", "track1");
            ContentServiceItemResponse itemResponse = (ContentServiceItemResponse) response.getEntity();
            assertNotNull(itemResponse);
            assertEquals(itemResponse.getTrackingId(), "track1");
            Record record = itemResponse.getRecordCollection().findRecord("1234");
            assertNotNull(record);
            assertEquals(record.getLocation(), "location");
        }
        System.out.println("Test getItemEntityEndpoint done!");
    }

    private void record(RecordCollection collection, String itemId, String status) {
        Record rec = collection.findRecord(itemId);
        rec.setAccessionDate(new Date());
        rec.setStatus(status);
        rec.setBranch("branch");
        rec.setDepartment("department");
        rec.setLocation("location");
        rec.setSubLocation("suplocation");
        rec.setCirculationRule("");
    }


}
