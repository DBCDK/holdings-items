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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import static org.junit.Assert.*;

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
    public void testGetItemEntityNoData() throws Exception {
        System.out.println("Test getItemEntity endpoint, no data...");
        try (Connection connection = dataSource.getConnection()) {
            Response response = contentResource.getItemEntity(654322, "1234", "track1");
            ContentServiceItemResponse itemResponse = (ContentServiceItemResponse) response.getEntity();
            assertNotNull(itemResponse);
            assertEquals(itemResponse.trackingId, "track1");
            assertEquals(itemResponse.holdings.size(), 0);
        }
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
            Response response = contentResource.getItemEntity(654321, "1234", "track1");
            ContentServiceItemResponse itemResponse = (ContentServiceItemResponse) response.getEntity();
            assertNotNull(itemResponse);
            assertEquals(itemResponse.trackingId, "track1");
            ResponseHoldingEntity rhe = itemResponse.holdings.get(0);
            assertEquals(rhe.itemId, "1234");
        }
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

            System.out.println("Calling service...");
            Response response = contentResource.getItemEntities(654321, Arrays.asList("hest:12345678"), "tracktwack");
            ContentServicePidResponse pidResponse = (ContentServicePidResponse) response.getEntity();
            assertNotNull(pidResponse);
            assertEquals(pidResponse.trackingId, "tracktwack");
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

            System.out.println("Calling service...");
            Response response = contentResource.getItemEntities(654321, Arrays.asList("hest:12345678", "fest:87654321"), "tracktwack");
            ContentServicePidResponse pidResponse = (ContentServicePidResponse) response.getEntity();
            assertNotNull(pidResponse);
            assertEquals(pidResponse.trackingId, "tracktwack");
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

            System.out.println("Calling service...");
            Response response = contentResource.getItemEntities(654321, Arrays.asList("hest:123456789"), "tracktwack");
            ContentServicePidResponse pidResponse = (ContentServicePidResponse) response.getEntity();
            assertNotNull(pidResponse);
            assertEquals(pidResponse.trackingId, "tracktwack");
            Map<String, List<ResponseHoldingEntity>> holdingsMap = pidResponse.holdings;
            assertTrue(holdingsMap.containsKey("hest:123456789"));
            List<ResponseHoldingEntity> holdings = holdingsMap.get("hest:123456789");
            assertNotNull(holdings);
            assertEquals(holdings.size(), 0);
        }
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
