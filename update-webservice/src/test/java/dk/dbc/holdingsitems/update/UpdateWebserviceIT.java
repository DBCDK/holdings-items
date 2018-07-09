/*
 * Copyright (C) 2017-2018 DBC A/S (http://dbc.dk/)
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
package dk.dbc.holdingsitems.update;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.testutils.postgres.connection.PostgresITDataSource;
import dk.dbc.forsrights.client.ForsRightsException;
import dk.dbc.holdingsitems.DatabaseMigrator;
import dk.dbc.oss.ns.holdingsitemsupdate.Authentication;
import dk.dbc.oss.ns.holdingsitemsupdate.BibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteBibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteHoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.Holding;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItem;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateResult;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateStatusEnum;
import dk.dbc.oss.ns.holdingsitemsupdate.ModificationTimeStamp;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineBibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineHoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.StatusType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class UpdateWebserviceIT {

    private PostgresITDataSource pgds;
    private DataSource dataSource;

    String branch = "My Branch";
    String department = "My Department";
    String location = "My Location";
    String subLocation = "My SubLocation";
    String circulationRule = "2Weeks";

    @Before
    public void setUp() throws SQLException {
        pgds = new PostgresITDataSource("holdingsitems");
        dataSource = pgds.getDataSource();

        try (Connection connection = dataSource.getConnection() ;
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DROP SCHEMA public CASCADE");
            stmt.executeUpdate("CREATE SCHEMA public");
        }
        DatabaseMigrator.migrate(dataSource);
    }

    @Test
    public void testHoldingsItemsUpdate() throws Exception {
        System.out.println("testHoldingsItemsUpdate");
        HoldingsItemsUpdateResult resp = mockUpdateWebservice()
                .holdingsItemsUpdate(updateReq1());
        System.out.println("status = " + resp.getHoldingsItemsUpdateStatusMessage());
        assertEquals("Expedcted success from request", HoldingsItemsUpdateStatusEnum.OK, resp.getHoldingsItemsUpdateStatus());
        assertEquals("Expected collections", 2, countAllCollections());
        assertEquals("Expected items", 3, countAllItems());
        assertEquals("Expected on shelf", 2, countItems(StatusType.ON_SHELF));
        assertEquals("Expected on order", 1, countItems(StatusType.ON_ORDER));
        HashMap<String, String> row = checkRow("101010", "12345678", "I1", "it1-1");
        assertNotNull("Expected a row", row);
        assertEquals("complete time same as original modified", "2017-09-07T09:09:00Z", row.get("c.complete"));
        System.out.println("OK");
    }

    @Test
    public void testHoldingsItemsUpdateQueue() throws Exception {
        System.out.println("testHoldingsItemsUpdateQueue");
        mockUpdateWebservice()
                .holdingsItemsUpdate(updateReq1());
        HashMap<String, Set<String>> queue = getQueue();
        assertEquals("queue size", 1, queue.get("updateOld").size());
        assertEquals("queue size", 1, queue.get("update").size());
        System.out.println("OK");
    }

    @Test
    public void testOnlineHoldingsItemsUpdate() throws Exception {
        System.out.println("testOnlineHoldingsItemsUpdate");
        HoldingsItemsUpdateResult respSetup = mockUpdateWebservice()
                .holdingsItemsUpdate(updateReq1());
        System.out.println("status = " + respSetup.getHoldingsItemsUpdateStatusMessage());
        assertEquals("Expedcted success from request", HoldingsItemsUpdateStatusEnum.OK, respSetup.getHoldingsItemsUpdateStatus());
        HoldingsItemsUpdateResult respCreate = mockUpdateWebservice()
                .onlineHoldingsItemsUpdate(onlineReqCreate());
        System.out.println("status = " + respCreate.getHoldingsItemsUpdateStatusMessage());
        assertEquals("Expedcted success from request", HoldingsItemsUpdateStatusEnum.OK, respCreate.getHoldingsItemsUpdateStatus());
        assertEquals("Expected collections", 3, countAllCollections());
        assertEquals("Expected items", 4, countAllItems());
        assertEquals("Expected online items", 1, countItems(StatusType.ONLINE));
        assertEquals("Expected online items", 0, countItems(StatusType.DECOMMISSIONED));
        System.out.println("OK");
        HoldingsItemsUpdateResult Delete = mockUpdateWebservice()
                .onlineHoldingsItemsUpdate(onlineReqDelete());
        System.out.println("status = " + Delete.getHoldingsItemsUpdateStatusMessage());
        assertEquals("Expedcted success from request", HoldingsItemsUpdateStatusEnum.OK, Delete.getHoldingsItemsUpdateStatus());
        System.out.println("OK");
    }

    @Test
    public void testOnlineHoldingsItemsUpdateQueue() throws Exception {
        System.out.println("testOnlineHoldingsItemsUpdateQueue");
        mockUpdateWebservice()
                .onlineHoldingsItemsUpdate(onlineReqCreate());
        HashMap<String, Set<String>> queue = getQueue();
        assertEquals("queue size", 1, queue.get("onlineOld").size());
        assertEquals("queue size", 1, queue.get("online").size());
        System.out.println("OK");
    }

    @Test
    public void testCompleteHoldingsItemsUpdateQueue() throws Exception {
        System.out.println("testCompleteHoldingsItemsUpdateQueue");
        mockUpdateWebservice()
                .completeHoldingsItemsUpdate(completeReq2());
        HashMap<String, Set<String>> queue = getQueue();
        assertEquals("queue size", 1, queue.get("completeOld").size());
        assertEquals("queue size", 1, queue.get("complete").size());
        System.out.println("OK");
    }

    @Test
    public void testUpdateHoldingsItemsCreateAndUpdateQueue() throws Exception {
        System.out.println("testCompleteHoldingsItemsUpdateQueue");
        mockUpdateWebservice()
                .holdingsItemsUpdate(updateReq1());
        getQueue(); // Just for logging

        try (Connection connection = dataSource.getConnection() ;
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("TRUNCATE q");
            stmt.executeUpdate("TRUNCATE queue");
        }

        mockUpdateWebservice()
                .holdingsItemsUpdate(updateReq2());
        HashMap<String, Set<String>> queue = getQueue();
        assertEquals("queue size", 2, queue.get("updateOld").size());
        assertEquals("queue size", 2, queue.get("update").size());
        String rec12345678str = queue.get("update").stream()
                .filter(s -> s.contains("12345678"))
                .findAny().orElseThrow(() -> new RuntimeException("Cannot find 12345678 record"));
        System.out.println("rec12345678 = " + rec12345678str);
        String rec87654321str = queue.get("update").stream()
                .filter(s -> s.contains("87654321"))
                .findAny().orElseThrow(() -> new RuntimeException("Cannot find 87654321 record"));
        System.out.println("rec87654321 = " + rec87654321str);
        ObjectMapper O = new ObjectMapper();
        JsonNode rec12345678 = O.readTree(rec12345678str.substring(rec12345678str.indexOf('{')));
        assertEquals("OnLoan", rec12345678.at("/it1-2/newStatus").asText(""));
        assertEquals("OnShelf", rec12345678.at("/it1-2/oldStatus").asText(""));
        JsonNode rec87654321 = O.readTree(rec87654321str.substring(rec87654321str.indexOf('{')));
        assertEquals("OnLoan", rec87654321.at("/it3-1/newStatus").asText(""));
        assertEquals("UNKNOWN", rec87654321.at("/it3-1/oldStatus").asText(""));
        System.out.println("OK");
    }

    @Test
    public void testCompleteHoldingsItemsCreateAndUpdateQueue() throws Exception {
        System.out.println("testCompleteHoldingsItemsUpdateQueue");
        mockUpdateWebservice()
                .completeHoldingsItemsUpdate(completeReq1());
        getQueue(); // Just for logging

        try (Connection connection = dataSource.getConnection() ;
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("TRUNCATE q");
            stmt.executeUpdate("TRUNCATE queue");
        }

        mockUpdateWebservice()
                .completeHoldingsItemsUpdate(completeReq3());
        HashMap<String, Set<String>> queue = getQueue();
        assertEquals("queue size", 1, queue.get("completeOld").size());
        assertEquals("queue size", 1, queue.get("complete").size());

        String rec12345678str = queue.get("complete").stream()
                .filter(s -> s.contains("12345678"))
                .findAny().orElseThrow(() -> new RuntimeException("Cannot find 12345678 record"));
        System.out.println("rec12345678 = " + rec12345678str);
        ObjectMapper O = new ObjectMapper();
        JsonNode rec12345678 = O.readTree(rec12345678str.substring(rec12345678str.indexOf('{')));
        assertEquals("OnLoan", rec12345678.at("/it3-2/newStatus").asText(""));
        assertEquals("UNKNOWN", rec12345678.at("/it3-2/oldStatus").asText(""));
        assertEquals("Decommissioned", rec12345678.at("/it3-1/newStatus").asText(""));
        assertEquals("OnLoan", rec12345678.at("/it3-1/oldStatus").asText(""));

        System.out.println("OK");
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testCompleteDecommissions() throws Exception {
        System.out.println("testCompleteDecommissions");
        HoldingsItemsUpdateResult respSetup = mockUpdateWebservice()
                .holdingsItemsUpdate(updateReq1());
        System.out.println("status = " + respSetup.getHoldingsItemsUpdateStatusMessage());
        assertEquals("Expedcted success from request", HoldingsItemsUpdateStatusEnum.OK, respSetup.getHoldingsItemsUpdateStatus());
        HoldingsItemsUpdateResult resp = mockUpdateWebservice()
                .completeHoldingsItemsUpdate(completeReq1());
        System.out.println("status = " + resp.getHoldingsItemsUpdateStatusMessage());
        assertEquals("Expedcted success from request", HoldingsItemsUpdateStatusEnum.OK, resp.getHoldingsItemsUpdateStatus());
        HoldingsItemsUpdateResult respOnline = mockUpdateWebservice()
                .onlineHoldingsItemsUpdate(onlineReqCreate());
        System.out.println("status = " + respOnline.getHoldingsItemsUpdateStatusMessage());
        assertEquals("Expedcted success from request", HoldingsItemsUpdateStatusEnum.OK, respOnline.getHoldingsItemsUpdateStatus());
        assertEquals("Expected collections", 4, countAllCollections());
        assertEquals("Expected items", 5, countAllItems());
        assertEquals("Expected decommissioned", 3, countItems(StatusType.DECOMMISSIONED));
        assertEquals("Expected online", 1, countItems(StatusType.ONLINE));
        HashMap<String, String> row = checkRow("101010", "12345678", "I1", "it1-1");
        assertNotNull("Expected a row", row);
        assertEquals("complete time as new update", "2017-09-07T09:09:00.001Z", row.get("c.complete"));
        System.out.println("OK");
    }

    public HoldingsItemsUpdateRequest updateReq1() throws DatatypeConfigurationException {
        return holdingsItemsUpdateRequest(
                "101010", null, "track-update-1",
                bibliographicItem(
                        "12345678", modified("2017-09-07T09:09:00.000Z"), "Some Note",
                        holding("I1", "Issue #1", date("2199-01-01"), 0,
                                item("it1-1", branch, department, location, subLocation, circulationRule,
                                     StatusType.ON_SHELF, date("2017-01-01")),
                                item("it1-2", branch, department, location, subLocation, circulationRule,
                                     StatusType.ON_SHELF, date("2017-01-01"))),
                        holding("I2", "Issue #2", null, 1,
                                item("it2-1", branch, department, location, subLocation, circulationRule,
                                     StatusType.ON_ORDER, date("2017-01-01")))
                ));
    }

    public HoldingsItemsUpdateRequest updateReq2() throws DatatypeConfigurationException {
        return holdingsItemsUpdateRequest(
                "101010", null, "track-update-2",
                bibliographicItem(
                        "12345678", modified("2017-09-07T09:09:00.001Z"), "Some Note",
                        holding("I1", "Issue #1", date("2199-01-01"), 0,
                                item("it1-1", branch, department, location, subLocation, circulationRule,
                                     StatusType.ON_SHELF, date("2017-01-01")),
                                item("it1-2", branch, department, location, subLocation, circulationRule,
                                     StatusType.ON_LOAN, date("2017-01-01"))), // Changed
                        holding("I2", "Issue #2", null, 1,
                                item("it2-1", branch, department, location, subLocation, circulationRule,
                                     StatusType.ON_ORDER, date("2017-01-01")))),
                bibliographicItem(
                        "87654321", modified("2017-09-07T09:09:00.001Z"), "Some Note",
                        holding("I1", "Issue #1", date("2199-01-01"), 0,
                                item("it3-1", branch, department, location, subLocation, circulationRule,
                                     StatusType.ON_LOAN, date("2017-01-01")))) // new
        );
    }

    private OnlineHoldingsItemsUpdateRequest onlineReqCreate() throws DatatypeConfigurationException {
        return onlineHoldingsItemsUpdateRequest(
                "101010", null, "track-online-1",
                onlineBibliographicItem("12345678", modified("2017-09-07T09:10:00.765Z"), true));
    }

    private OnlineHoldingsItemsUpdateRequest onlineReqDelete() throws DatatypeConfigurationException {
        return onlineHoldingsItemsUpdateRequest(
                "101010", null, "track-online-2",
                onlineBibliographicItem("12345678", modified("2017-09-07T09:10:21.765Z"), false));
    }

    private CompleteHoldingsItemsUpdateRequest completeReq1() throws DatatypeConfigurationException {
        return completeHoldingsItemsUpdateRequest(
                "101010", null, "track-complete-1",
                completeBibliographicItem(
                        "12345678", modified("2017-09-07T09:09:00.001Z"), "Other Note",
                        holding("I3", "Issue #3", null, 1,
                                item("it3-1", branch, department, location, subLocation, circulationRule,
                                     StatusType.ON_LOAN, date("2017-01-01")))));
    }

    private CompleteHoldingsItemsUpdateRequest completeReq2() throws DatatypeConfigurationException {
        return completeHoldingsItemsUpdateRequest(
                "101010", null, "track-complete-1",
                completeBibliographicItem(
                        "12345678", modified("2017-09-07T09:09:00.001Z"), "Other Note",
                        holding("I3", "Issue #3", null, 1,
                                item("it3-1", branch, department, location, subLocation, circulationRule,
                                     StatusType.ON_LOAN, date("2017-01-01"))),
                        holding("I4", "Issue #4", null, 1,
                                item("it4-1", branch, department, location, subLocation, circulationRule,
                                     StatusType.ON_LOAN, date("2017-01-01")))));
    }

    private CompleteHoldingsItemsUpdateRequest completeReq3() throws DatatypeConfigurationException {
        return completeHoldingsItemsUpdateRequest(
                "101010", null, "track-complete-1",
                completeBibliographicItem(
                        "12345678", modified("2017-09-07T09:09:00.001Z"), "Other Note",
                        holding("I3", "Issue #3", null, 1,
                                item("it3-1", branch, department, location, subLocation, circulationRule,
                                     StatusType.ON_LOAN, date("2017-01-01"))),
                        holding("I3", "Issue #3", null, 1,
                                item("it3-2", branch, department, location, subLocation, circulationRule,
                                     StatusType.ON_LOAN, date("2017-01-01")))));
    }

    private UpdateWebservice mockUpdateWebservice() throws SQLException, ForsRightsException {
        UpdateWebservice mock = mock(UpdateWebservice.class);
        mock.config = mockConfig();
        mock.requestCounter = mock(Counter.class);
        mock.requestUpdateCounter = mock(Counter.class);
        mock.requestCompleteCounter = mock(Counter.class);
        mock.requestOnlineCounter = mock(Counter.class);
        mock.requestInvalidCounter = mock(Counter.class);
        mock.requestSystemError = mock(Counter.class);
        mock.requestAuthenticationError = mock(Counter.class);
        mock.saveCollectionTimer = mock(Timer.class);
        mock.loadCollectionTimer = mock(Timer.class);
        mock.validator = mock(AccessValidator.class);
        doReturn(true).when(mock.validator).validate(anyObject(), anyString(), anyString(), anyString());
        mock.wsc = mock(WebServiceContext.class);
        doReturn(mockMessageContext()).when(mock.wsc).getMessageContext();

        doCallRealMethod().when(mock).holdingsItemsUpdate(anyObject());
        doCallRealMethod().when(mock).completeHoldingsItemsUpdate(anyObject());
        doCallRealMethod().when(mock).onlineHoldingsItemsUpdate(anyObject());
        when(mock.getUTCConnection()).then(i -> getExtraConnection());
        return mock;
    }

    private Connection getExtraConnection() throws SQLException {
        Connection connection = dataSource.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement("SET TIME ZONE 'UTC'")) {
            stmt.executeUpdate();
        }
        connection.setAutoCommit(false);
        return connection;
    }

    private HashMap<String, Set<String>> getQueue() throws SQLException {
        HashMap<String, Set<String>> result = new HashMap<>();
        try (Connection connection = dataSource.getConnection() ;
             PreparedStatement stmt = connection.prepareStatement("SELECT worker, agencyId, bibliographicRecordId, issueId FROM q") ;
             ResultSet resultSet = stmt.executeQuery()) {
            while (resultSet.next()) {
                result.computeIfAbsent(resultSet.getString(1), s -> new HashSet<>())
                        .add(resultSet.getInt(2) + "|" +
                             resultSet.getString(3) + "|" +
                             resultSet.getString(4));

            }
        }
        try (Connection connection = dataSource.getConnection() ;
             PreparedStatement stmt = connection.prepareStatement("SELECT consumer, agencyId, bibliographicRecordId, stateChange FROM queue") ;
             ResultSet resultSet = stmt.executeQuery()) {
            while (resultSet.next()) {
                result.computeIfAbsent(resultSet.getString(1), s -> new HashSet<>())
                        .add(resultSet.getInt(2) + "|" +
                             resultSet.getString(3) + "|" +
                             resultSet.getString(4));
            }
        }
        System.out.println("queue = " + result);
        return result;
    }

    private MessageContext mockMessageContext() {
        MessageContext mock = mock(MessageContext.class);
        return mock;
    }

    private Config mockConfig() {
        Config mock = new Config();
        mock.disableAuthentication = true;
        mock.updateQueueListReal = "updateOld:old,update";
        mock.completeQueueListReal = "completeOld:old,complete";
        mock.onlineQueueListReal = "onlineOld:old,online";
        mock.init();
        return mock;
    }

    private HoldingsItemsUpdateRequest holdingsItemsUpdateRequest(String agencyId, Authentication authentication, String trackingId, BibliographicItem... bibliographicItems) {
        HoldingsItemsUpdateRequest req = new HoldingsItemsUpdateRequest();
        req.setAgencyId(agencyId);
        req.setAuthentication(authentication);
        req.setTrackingId(trackingId);
        req.getBibliographicItems().addAll(Arrays.asList(bibliographicItems));
        return req;
    }

    private CompleteHoldingsItemsUpdateRequest completeHoldingsItemsUpdateRequest(String agencyId, Authentication authentication, String trackingId, CompleteBibliographicItem bibliographicItem) {
        CompleteHoldingsItemsUpdateRequest req = new CompleteHoldingsItemsUpdateRequest();
        req.setAgencyId(agencyId);
        req.setAuthentication(authentication);
        req.setTrackingId(trackingId);
        req.setCompleteBibliographicItem(bibliographicItem);
        return req;
    }

    private OnlineHoldingsItemsUpdateRequest onlineHoldingsItemsUpdateRequest(String agencyId, Authentication authentication, String trackingId, OnlineBibliographicItem... bibliographicItems) {
        OnlineHoldingsItemsUpdateRequest req = new OnlineHoldingsItemsUpdateRequest();
        req.setAgencyId(agencyId);
        req.setAuthentication(authentication);
        req.setTrackingId(trackingId);
        req.getOnlineBibliographicItems().addAll(Arrays.asList(bibliographicItems));
        return req;
    }

    private BibliographicItem bibliographicItem(String bibliographicRecordId, ModificationTimeStamp modified, String note, Holding... holdings) {
        BibliographicItem bibl = new BibliographicItem();
        bibl.setBibliographicRecordId(bibliographicRecordId);
        bibl.setModificationTimeStamp(modified);
        bibl.setNote(note);
        bibl.getHoldings().addAll(Arrays.asList(holdings));
        return bibl;
    }

    private CompleteBibliographicItem completeBibliographicItem(String bibliographicRecordId, ModificationTimeStamp modified, String note, Holding... holdings) {
        CompleteBibliographicItem bibl = new CompleteBibliographicItem();
        bibl.setBibliographicRecordId(bibliographicRecordId);
        bibl.setModificationTimeStamp(modified);
        bibl.setNote(note);
        bibl.getHoldings().addAll(Arrays.asList(holdings));
        return bibl;
    }

    private OnlineBibliographicItem onlineBibliographicItem(String bibliographicRecordId, ModificationTimeStamp modified, boolean hasOnlineHoldings) {
        OnlineBibliographicItem bibl = new OnlineBibliographicItem();
        bibl.setBibliographicRecordId(bibliographicRecordId);
        bibl.setModificationTimeStamp(modified);
        bibl.setHasOnlineHolding(hasOnlineHoldings);
        return bibl;
    }

    private Holding holding(String issueId, String issueText, XMLGregorianCalendar expectedDeliveryDate, int readyForLoan, HoldingsItem... items) {
        Holding hold = new Holding();
        hold.setIssueId(issueId);
        hold.setIssueText(issueText);
        hold.setExpectedDeliveryDate(expectedDeliveryDate);
        hold.setReadyForLoan(BigInteger.valueOf(readyForLoan));
        hold.getHoldingsItems().addAll(Arrays.asList(items));
        return hold;
    }

    private HoldingsItem item(String itemId, String branch, String department, String location, String subLocation, String circulationRule, StatusType status, XMLGregorianCalendar accessionDate) {
        HoldingsItem item = new HoldingsItem();
        item.setItemId(itemId);
        item.setBranch(branch);
        item.setDepartment(department);
        item.setLocation(location);
        item.setSubLocation(subLocation);
        item.setCirculationRule(circulationRule);
        item.setStatus(status);
        item.setAccessionDate(accessionDate);
        return item;
    }

    private XMLGregorianCalendar date(String date) throws DatatypeConfigurationException {
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(date + "T00:00:00.000Z");
    }

    private ModificationTimeStamp modified(String time) throws DatatypeConfigurationException {
        ModificationTimeStamp mod = new ModificationTimeStamp();
        XMLGregorianCalendar xmlCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(time);
        int millis = xmlCalendar.getFractionalSecond().multiply(new BigDecimal(1000)).intValue();
        xmlCalendar.setFractionalSecond(BigDecimal.ZERO);
        mod.setModificationDateTime(xmlCalendar);
        mod.setModificationMilliSeconds(millis);
        return mod;
    }

    private Authentication authentication(String user, String group, String password) {
        Authentication auth = new Authentication();
        auth.setUserIdAut(user);
        auth.setGroupIdAut(group);
        auth.setPasswordAut(password);
        return auth;
    }

    private int countAllCollections() throws SQLException {
        try (Connection db = dataSource.getConnection() ;
             PreparedStatement stmt = db.prepareStatement("SELECT COUNT(*) FROM holdingsitemscollection") ;
             ResultSet resultSet = stmt.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return -1;
    }

    private int countAllItems() throws SQLException {
        try (Connection db = dataSource.getConnection() ;
             PreparedStatement stmt = db.prepareStatement("SELECT COUNT(*) FROM holdingsitemsitem") ;
             ResultSet resultSet = stmt.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return -1;
    }

    private int countItems(StatusType type) throws SQLException {
        try (Connection db = dataSource.getConnection() ;
             PreparedStatement stmt = db.prepareStatement("SELECT COUNT(*) FROM holdingsitemsitem WHERE status=?")) {
            stmt.setString(1, type.value());
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return -1;
    }

    private HashMap<String, String> checkRow(String agencyId, String bibliographicRecordId, String issueId, String itemId) throws SQLException {
        try (Connection db = dataSource.getConnection() ;
             PreparedStatement stmt = db.prepareStatement(
                     "SELECT " +
                     "c.agencyid, " +
                     "c.bibliographicrecordid, " +
                     "c.issueid, " +
                     "c.issuetext, " +
                     "c.expecteddelivery, " +
                     "c.readyforloan, " +
                     "c.note, " +
                     "c.complete, " +
                     "c.created, " +
                     "c.modified, " +
                     "c.trackingid, " +
                     "i.agencyid, " +
                     "i.bibliographicrecordid, " +
                     "i.issueid, " +
                     "i.itemid, " +
                     "i.branch, " +
                     "i.department, " +
                     "i.location, " +
                     "i.sublocation, " +
                     "i.circulationrule, " +
                     "i.accessiondate, " +
                     "i.status, " +
                     "i.created, " +
                     "i.modified, " +
                     "i.trackingid " +
                     "FROM holdingsitemscollection AS c JOIN holdingsitemsitem AS i USING (" +
                     "agencyid, " +
                     "bibliographicrecordid, " +
                     "issueid" +
                     ") WHERE " +
                     "agencyid=? AND " +
                     "bibliographicrecordid=? AND " +
                     "issueid=? AND " +
                     "itemid=? ")) {
            stmt.setInt(1, Integer.parseInt(agencyId));
            stmt.setString(2, bibliographicRecordId);
            stmt.setString(3, issueId);
            stmt.setString(4, itemId);
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    AtomicInteger i = new AtomicInteger(0);
                    HashMap<String, String> row = new HashMap<>();
                    setInt("c.agencyid", resultSet, i, row);
                    setString("c.bibliographicrecordid", resultSet, i, row);
                    setString("c.issueid", resultSet, i, row);
                    setString("c.issuetext", resultSet, i, row);
                    setDate("c.expecteddelivery", resultSet, i, row);
                    setInt("c.readyforloan", resultSet, i, row);
                    setString("c.note", resultSet, i, row);
                    setTimestamp("c.complete", resultSet, i, row);
                    setTimestamp("c.created", resultSet, i, row);
                    setTimestamp("c.modified", resultSet, i, row);
                    setString("c.trackingid", resultSet, i, row);
                    setString("i.agencyid", resultSet, i, row);
                    setString("i.bibliographicrecordid", resultSet, i, row);
                    setString("i.issueid", resultSet, i, row);
                    setString("i.itemid", resultSet, i, row);
                    setString("i.branch", resultSet, i, row);
                    setString("i.department", resultSet, i, row);
                    setString("i.location", resultSet, i, row);
                    setString("i.sublocation", resultSet, i, row);
                    setString("i.circulationrule", resultSet, i, row);
                    setDate("i.accessiondate", resultSet, i, row);
                    setString("i.status", resultSet, i, row);
                    setTimestamp("i.created", resultSet, i, row);
                    setTimestamp("i.modified", resultSet, i, row);
                    setString("i.trackingid", resultSet, i, row);
                    System.out.println("row = " + row);
                    return row;
                }
            }
        }
        System.out.println("row = null");
        return null;
    }

    private void setInt(String key, ResultSet resultSet, AtomicInteger i, HashMap<String, String> ret) throws SQLException {
        ret.put(key, String.valueOf(resultSet.getInt(i.incrementAndGet())));
    }

    private void setString(String key, ResultSet resultSet, AtomicInteger i, HashMap<String, String> ret) throws SQLException {
        ret.put(key, resultSet.getString(i.incrementAndGet()));
    }

    private void setDate(String key, ResultSet resultSet, AtomicInteger i, HashMap<String, String> ret) throws SQLException {
        ret.put(key, resultSet.getDate(i.incrementAndGet()).toString());
    }

    private void setTimestamp(String key, ResultSet resultSet, AtomicInteger i, HashMap<String, String> ret) throws SQLException {
        ret.put(key, resultSet.getTimestamp(i.incrementAndGet())
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .toInstant(ZoneOffset.UTC)
                .toString());
    }

}
