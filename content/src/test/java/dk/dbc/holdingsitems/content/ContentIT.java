package dk.dbc.holdingsitems.content;

import dk.dbc.commons.testutils.postgres.connection.PostgresITDataSource;
import dk.dbc.holdingsitems.DatabaseMigrator;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.Record;
import dk.dbc.holdingsitems.RecordCollection;
import dk.dbc.holdingsitems.content.response.ContentServiceItemResponse;
import dk.dbc.holdingsitems.content.response.ContentServicePidResponse;
import dk.dbc.holdingsitems.content.response.ResponseHoldingEntity;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ContentIT {
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
    public void testGetItemEntityNoData() throws Exception {
        System.out.println("Test getItemEntity endpoint, no data...");
        Response response = contentResource.getItemEntity(654322, "1234", "track1");
        ContentServiceItemResponse itemResponse = (ContentServiceItemResponse) response.getEntity();
        assertNotNull(itemResponse);
        assertEquals(itemResponse.trackingId, "track1");
        assertEquals(itemResponse.holdings.size(), 0);
    }

    @Test(timeout = 30_000L)
    public void testGetItemEntityNoAgencyFound() throws Exception {
        System.out.println("Test getItemEntity endpoint, non-existing agency...");
        try (Connection connection = dataSource.getConnection()) {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, "track1", true);
            RecordCollection issue1 = dao.getRecordCollection("12345678", 654321, "Issue#1");
            issue1.setIssueText("#1");
            issue1.setNote("");
            issue1.setReadyForLoan(1);
            record(issue1, "1234", "OnShelf");
            record(issue1, "2345", "OnLoan");
            issue1.save(Timestamp.from(Instant.now()));
            Response response = contentResource.getItemEntity(654322, "1234", "track1");
            ContentServiceItemResponse itemResponse = (ContentServiceItemResponse) response.getEntity();
            assertNotNull(itemResponse);
            assertEquals(itemResponse.trackingId, "track1");
            assertEquals(itemResponse.holdings.size(), 0);
        }
    }

    @Test(timeout = 30_000L)
    public void testGetItemEntityNoItemFound() throws Exception {
        System.out.println("Test getItemEntity endpoint, non-existing item id...");
        try (Connection connection = dataSource.getConnection()) {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, "track1", true);
            RecordCollection issue1 = dao.getRecordCollection("12345678", 654321, "Issue#1");
            issue1.setIssueText("#1");
            issue1.setNote("");
            issue1.setReadyForLoan(1);
            record(issue1, "1234", "OnShelf");
            record(issue1, "2345", "OnLoan");
            issue1.save(Timestamp.from(Instant.now()));
            Response response = contentResource.getItemEntity(654321, "1243", "track1");
            ContentServiceItemResponse itemResponse = (ContentServiceItemResponse) response.getEntity();
            assertNotNull(itemResponse);
            assertEquals(itemResponse.trackingId, "track1");
            assertEquals(itemResponse.holdings.size(), 0);
        }
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
            Response response = contentResource.getItemEntity(654321, "1234", "trackItemEntity");
            ContentServiceItemResponse itemResponse = (ContentServiceItemResponse) response.getEntity();
            assertNotNull(itemResponse);
            assertEquals(itemResponse.trackingId, "trackItemEntity");
            ResponseHoldingEntity rhe = itemResponse.holdings.get(0);
            assertEquals(rhe.itemId, "1234");
        }
    }

    @Test(timeout = 30_000L)
    public void testGetItemEntitiesNoAgency() throws Exception {
        System.out.println("Test getItemEntity endpoint, no agency argument");
        Response response = contentResource.getItemEntity(null, "123456", "trackItemNoAgency");
        assertEquals(response.getStatusInfo().getStatusCode(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test(timeout = 30_000L)
    public void testGetItemEntitiesNoItem() throws Exception {
        System.out.println("Test getItemEntity endpoint, no itemId argument");
        Response response = contentResource.getItemEntity(654321, "", "trackItemNoItem");
        assertEquals(response.getStatusInfo().getStatusCode(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test(timeout = 30_000L)
    public void testGetPidEntitiesOnePid() throws Exception {
        System.out.println("Test getItemByPid endpoint, one pid");
        try (Connection connection = dataSource.getConnection()) {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, "track1", true);
            RecordCollection issue1 = dao.getRecordCollection("12345678", 654321, "Issue#1");
            issue1.setIssueText("#1");
            issue1.setNote("");
            issue1.setReadyForLoan(1);
            record(issue1, "1234", "OnShelf");
            record(issue1, "2345", "OnLoan");
            issue1.save(Timestamp.from(Instant.now()));

            Response response = contentResource.getItemEntities(654321, Arrays.asList("hest:12345678"), "trackPidOnePid");
            ContentServicePidResponse pidResponse = (ContentServicePidResponse) response.getEntity();
            assertNotNull(pidResponse);
            assertEquals(pidResponse.trackingId, "trackPidOnePid");
            Map<String, List<ResponseHoldingEntity>> holdingsMap = pidResponse.holdings;
            assertTrue(holdingsMap.containsKey("hest:12345678"));
            List<ResponseHoldingEntity> holdings = holdingsMap.get("hest:12345678");
            assertNotNull(holdings);
            assertEquals(holdings.size(), 2);
            List<ResponseHoldingEntity> heOnShelfList = holdings.stream().filter(h -> h.status.equalsIgnoreCase("OnShelf")).collect(Collectors.toList());
            assertEquals(heOnShelfList.size(), 1);
            ResponseHoldingEntity heOnShelf = heOnShelfList.get(0);
            assertNotNull(heOnShelf);
            assertEquals(heOnShelf.itemId, "1234");
        }
    }

    @Test(timeout = 30_000L)
    public void testGetPidEntitiesTwoPids() throws Exception {
        System.out.println("Test getItemByPid endpoint, two pids");
        try (Connection connection = dataSource.getConnection()) {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, "track1", true);
            RecordCollection issue1 = dao.getRecordCollection("12345678", 654321, "Issue#1");
            issue1.setIssueText("#1");
            issue1.setNote("");
            issue1.setReadyForLoan(1);
            record(issue1, "1234", "OnShelf");
            record(issue1, "2345", "OnLoan");
            issue1.save(Timestamp.from(Instant.now()));

            RecordCollection issue2 = dao.getRecordCollection("87654321", 654321, "Issue#1");
            issue2.setIssueText("#2");
            issue2.setNote("");
            issue2.setReadyForLoan(1);
            record(issue2, "4321", "OnShelf");
            issue2.save(Timestamp.from(Instant.now()));

            Response response = contentResource.getItemEntities(654321, Arrays.asList("hest:12345678", "fest:87654321"), "trackPidTwoPids");
            ContentServicePidResponse pidResponse = (ContentServicePidResponse) response.getEntity();
            assertNotNull(pidResponse);
            assertEquals(pidResponse.trackingId, "trackPidTwoPids");
            Map<String, List<ResponseHoldingEntity>> holdingsMap = pidResponse.holdings;
            assertTrue(holdingsMap.containsKey("hest:12345678"));
            List<ResponseHoldingEntity> holdingsHest = holdingsMap.get("hest:12345678");
            assertNotNull(holdingsHest);
            assertEquals(holdingsHest.size(), 2);
            List<ResponseHoldingEntity> heOnShelfList = holdingsHest.stream().filter(h -> h.status.equalsIgnoreCase("OnShelf")).collect(Collectors.toList());
            assertEquals(heOnShelfList.size(), 1);
            ResponseHoldingEntity heOnShelf = heOnShelfList.get(0);
            assertNotNull(heOnShelf);
            assertEquals(heOnShelf.itemId, "1234");

            assertTrue(holdingsMap.containsKey("fest:87654321"));
            List<ResponseHoldingEntity> holdingsFest = holdingsMap.get("fest:87654321");
            assertNotNull(holdingsFest);
            assertEquals(holdingsFest.size(), 1);
            ResponseHoldingEntity heFest = holdingsFest.get(0);
            assertNotNull(heFest);
            assertEquals(heFest.itemId, "4321");
        }
    }

    @Test(timeout = 30_000L)
    public void testGetPidEntitiesNonExistingPid() throws Exception {
        System.out.println("Test getItemByPid endpoint, non-existing pid");
        try (Connection connection = dataSource.getConnection()) {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, "track1", true);
            RecordCollection issue1 = dao.getRecordCollection("12345678", 654321, "Issue#1");
            issue1.setIssueText("#1");
            issue1.setNote("");
            issue1.setReadyForLoan(1);
            record(issue1, "1234", "OnShelf");
            record(issue1, "2345", "OnLoan");
            issue1.save(Timestamp.from(Instant.now()));

            Response response = contentResource.getItemEntities(654321, Arrays.asList("hest:123456789"), "trackPidNonExistingPid");
            ContentServicePidResponse pidResponse = (ContentServicePidResponse) response.getEntity();
            assertNotNull(pidResponse);
            assertEquals(pidResponse.trackingId, "trackPidNonExistingPid");
            Map<String, List<ResponseHoldingEntity>> holdingsMap = pidResponse.holdings;
            assertTrue(holdingsMap.containsKey("hest:123456789"));
            List<ResponseHoldingEntity> holdings = holdingsMap.get("hest:123456789");
            assertNotNull(holdings);
            assertEquals(holdings.size(), 0);
        }
    }

    @Test(timeout = 30_000L)
    public void testGetPidEntitiesRepeatedPid() throws Exception {
        System.out.println("Test getItemByPid endpoint, repeated pid");
        Response response = contentResource.getItemEntities(654321, Arrays.asList("hest:12345678", "fest:12345678"), "trackPidRepeatedPid");
        assertEquals(response.getStatusInfo().getStatusCode(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test(timeout = 30_000L)
    public void testGetPidEntitiesNoAgency() throws Exception {
        System.out.println("Test getItemByPid endpoint, no agency");
        Response response = contentResource.getItemEntities(null, Arrays.asList("hest:12345678"), "trackPidNoAgency");
        assertEquals(response.getStatusInfo().getStatusCode(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    private void record(RecordCollection collection, String itemId, String status) {
        Record rec = collection.findRecord(itemId);
        rec.setAccessionDate(new Date());
        rec.setStatus(status);
        rec.setBranch("branch");
        rec.setDepartment("department");
        rec.setLocation("location");
        rec.setSubLocation("sublocation");
        rec.setCirculationRule("");
    }
}
