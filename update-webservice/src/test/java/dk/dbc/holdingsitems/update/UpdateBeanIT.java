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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.LoanRestriction;
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
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.persistence.EntityManager;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class UpdateBeanIT extends JpaBase {

    private static final ObjectMapper O = new ObjectMapper();

    String branch = "My Branch";
    String department = "My Department";
    String location = "My Location";
    String subLocation = "My SubLocation";
    String circulationRule = "2Weeks";

    @Test(timeout = 10_000L)
    public void testHoldingsItemsUpdate() throws Exception {
        System.out.println("testHoldingsItemsUpdate");
        jpa(em -> {
            HoldingsItemsUpdateResult resp = mockUpdateBean(em)
                    .holdingsItemsUpdate(updateReq1());
            System.out.println("status = " + resp.getHoldingsItemsUpdateStatusMessage());
            assertEquals("Expedcted success from request", HoldingsItemsUpdateStatusEnum.OK, resp.getHoldingsItemsUpdateStatus());
        });
        assertEquals("Expected collections", 2, countAllIssues());
        assertEquals("Expected items", 3, countAllItems());
        assertEquals("Expected on shelf", 2, countItems(StatusType.ON_SHELF));
        assertEquals("Expected on order", 1, countItems(StatusType.ON_ORDER));

        HashMap<String, String> row = checkRow(101010, "12345678", "I1", "it1-1");
        assertNotNull("Expected a row", row);
        assertEquals("complete time same as original modified", "2017-09-07T09:09:00Z", row.get("c.complete"));
        System.out.println("OK");
    }

    @Test(timeout = 10_000L)
    public void testHoldingsItemsUpdateQueue() throws Exception {
        System.out.println("testHoldingsItemsUpdateQueue");
        jpa(em -> {
            mockUpdateBean(em)
                    .holdingsItemsUpdate(updateReq1());
        });
        HashMap<String, Set<String>> queue = getQueue();
        assertEquals("queue size", 1, queue.get("update").size());
        System.out.println("OK");
    }

    @Test(timeout = 10_000L)
    public void testOnlineHoldingsItemsUpdate() throws Exception {
        System.out.println("testOnlineHoldingsItemsUpdate");
        jpa(em -> {
            HoldingsItemsUpdateResult respSetup = mockUpdateBean(em)
                    .holdingsItemsUpdate(updateReq1());
            System.out.println("status = " + respSetup.getHoldingsItemsUpdateStatusMessage());
            assertEquals("Expedcted success from request", HoldingsItemsUpdateStatusEnum.OK, respSetup.getHoldingsItemsUpdateStatus());
        });
        jpa(em -> {
            HoldingsItemsUpdateResult respCreate = mockUpdateBean(em)
                    .onlineHoldingsItemsUpdate(onlineReqCreate());
            System.out.println("status = " + respCreate.getHoldingsItemsUpdateStatusMessage());
            assertEquals("Expedcted success from request", HoldingsItemsUpdateStatusEnum.OK, respCreate.getHoldingsItemsUpdateStatus());
        });
        assertEquals("Expected collections", 3, countAllIssues());
        assertEquals("Expected items", 4, countAllItems());
        assertEquals("Expected online items", 1, countItems(StatusType.ONLINE));
        assertEquals("Expected online items", 0, countItems(StatusType.DECOMMISSIONED));
        jpa(em -> {
            HoldingsItemsUpdateResult delete = mockUpdateBean(em)
                    .onlineHoldingsItemsUpdate(onlineReqDelete());
            System.out.println("status = " + delete.getHoldingsItemsUpdateStatusMessage());
            assertEquals("Expedcted success from request", HoldingsItemsUpdateStatusEnum.OK, delete.getHoldingsItemsUpdateStatus());
        });
        System.out.println("OK");
    }

    @Test(timeout = 10_000L)
    public void testOnlineHoldingsItemsUpdateQueue() throws Exception {
        System.out.println("testOnlineHoldingsItemsUpdateQueue");
        jpa(em -> {
            mockUpdateBean(em)
                    .onlineHoldingsItemsUpdate(onlineReqCreate());
        });
        HashMap<String, Set<String>> queue = getQueue();
        assertEquals("queue size", 1, queue.get("online").size());
        System.out.println("OK");
    }

    @Test(timeout = 10_000L)
    public void testCompleteHoldingsItemsUpdateQueue() throws Exception {
        System.out.println("testCompleteHoldingsItemsUpdateQueue");
        jpa(em -> {
            mockUpdateBean(em)
                    .completeHoldingsItemsUpdate(completeReq2());
        });
        HashMap<String, Set<String>> queue = getQueue();
        assertEquals("queue size", 1, queue.get("complete").size());
        System.out.println("OK");
    }

    @Test(timeout = 10_000L)
    public void testUpdateHoldingsItemsCreateAndUpdateQueue() throws Exception {
        System.out.println("testCompleteHoldingsItemsUpdateQueue");
        jpa(em -> {
            mockUpdateBean(em)
                    .holdingsItemsUpdate(updateReq1());
        });
        getQueue(); // Just for logging
        clearQueue();

        jpa(em -> {
            mockUpdateBean(em)
                    .holdingsItemsUpdate(updateReq2());
        });
        HashMap<String, Set<String>> queue = getQueue();
        assertEquals("queue size", 2, queue.get("update").size());
        String rec12345678str = queue.get("update").stream()
                .filter(s -> s.contains("12345678"))
                .findAny().orElseThrow(() -> new RuntimeException("Cannot find 12345678 record"));
        System.out.println("rec12345678 = " + rec12345678str);
        String rec87654321str = queue.get("update").stream()
                .filter(s -> s.contains("87654321"))
                .findAny().orElseThrow(() -> new RuntimeException("Cannot find 87654321 record"));
        System.out.println("rec87654321 = " + rec87654321str);
        JsonNode rec12345678 = O.readTree(rec12345678str.substring(rec12345678str.indexOf('{')));
        assertEquals("OnLoan", rec12345678.at("/it1-2/newStatus").asText(""));
        assertEquals("OnShelf", rec12345678.at("/it1-2/oldStatus").asText(""));
        JsonNode rec87654321 = O.readTree(rec87654321str.substring(rec87654321str.indexOf('{')));
        assertEquals("OnLoan", rec87654321.at("/it3-1/newStatus").asText(""));
        assertEquals("UNKNOWN", rec87654321.at("/it3-1/oldStatus").asText(""));
    }

    @Test(timeout = 10_000L)
    public void testUpdateHoldingsItemsFromKnownToDecommissioned() throws Exception {
        System.out.println("testUpdateHoldingsItemsFromKnownToDecommissioned");
        jpa(em -> {
            mockUpdateBean(em)
                    .holdingsItemsUpdate(holdingsItemsUpdateRequest(
                            101010, null, "track-update-2",
                            bibliographicItem("12345678", modified("2017-09-07T09:09:01.000Z"), "note",
                                              holding("i1", "issue1", date("2222-11-11"), 0,
                                                      item("i1", "b", "123456", "dep", "loc", "sloc", "cr", StatusType.ON_SHELF, date("1911-01-01"))))
                    ));
        });
        System.out.println("getQueue() = " + getQueue());
        clearQueue();
        jpa(em -> {
            mockUpdateBean(em)
                    .holdingsItemsUpdate(holdingsItemsUpdateRequest(
                            101010, null, "track-update-3",
                            bibliographicItem("12345678", modified("2018-09-07T09:09:01.000Z"), "note",
                                              holding("i1", "issue1", date("2222-11-11"), 0,
                                                      item("i1", "b", "123456", "dep", "loc", "sloc", "cr", StatusType.DECOMMISSIONED, date("1911-01-01"))))
                    ));
        });
        HashMap<String, Set<String>> queue = getQueue();
        assertEquals("queue size", 1, queue.get("update").size());

        String rec12345678str = queue.get("update").stream()
                .filter(s -> s.contains("12345678"))
                .findAny().orElseThrow(() -> new RuntimeException("Cannot find 12345678 record"));
        System.out.println("rec12345678 = " + rec12345678str);
        JsonNode rec12345678 = O.readTree(rec12345678str.substring(rec12345678str.indexOf('{'), rec12345678str.lastIndexOf('}') + 1));
        System.out.println("rec12345678 = " + rec12345678);
        assertEquals("Decommissioned", rec12345678.at("/i1/newStatus").asText(""));
        assertEquals("OnShelf", rec12345678.at("/i1/oldStatus").asText(""));
    }

    @Test(timeout = 10_000L)
    public void testUpdateHoldingsItemsFromUnknownToDecommissioned() throws Exception {
        System.out.println("testUpdateHoldingsItemsFromUnknownToDecommissioned");
        clearQueue();
        jpa(em -> {
            mockUpdateBean(em)
                    .holdingsItemsUpdate(holdingsItemsUpdateRequest(
                            101010, null, "track-update-2",
                            bibliographicItem("12345678", modified("2017-09-07T09:09:01.000Z"), "note",
                                              holding("i1", "issue1", date("2222-11-11"), 0,
                                                      item("i1", "b", "123456", "dep", "loc", "sloc", "cr", StatusType.DECOMMISSIONED, date("1911-01-01"))))
                    ));
        });
        HashMap<String, Set<String>> queue = getQueue();
        assertEquals("queue size", 1, queue.get("update").size());

        String rec12345678str = queue.get("update").stream()
                .filter(s -> s.contains("12345678"))
                .findAny().orElseThrow(() -> new RuntimeException("Cannot find 12345678 record"));
        System.out.println("rec12345678 = " + rec12345678str);
        JsonNode rec12345678 = O.readTree(rec12345678str.substring(rec12345678str.indexOf('{'), rec12345678str.lastIndexOf('}') + 1));
        System.out.println("rec12345678 = " + rec12345678);
        assertEquals(null, rec12345678.get("i1"));
    }

    @Test(timeout = 10_000L)
    public void testCompleteHoldingsItemsCreateAndUpdateQueue() throws Exception {
        System.out.println("testCompleteHoldingsItemsUpdateQueue");
        jpa(em -> {
            mockUpdateBean(em)
                    .completeHoldingsItemsUpdate(completeReq1());
        });
        getQueue(); // Just for logging
        clearQueue();
        jpa(em -> {
            mockUpdateBean(em)
                    .completeHoldingsItemsUpdate(completeReq3());
        });
        HashMap<String, Set<String>> queue = getQueue();
        assertEquals("queue size", 1, queue.get("complete").size());

        String rec12345678str = queue.get("complete").stream()
                .filter(s -> s.contains("12345678"))
                .findAny().orElseThrow(() -> new RuntimeException("Cannot find 12345678 record"));
        System.out.println("rec12345678 = " + rec12345678str);
        JsonNode rec12345678 = O.readTree(rec12345678str.substring(rec12345678str.indexOf('{')));
        assertEquals("OnLoan", rec12345678.at("/it3-2/newStatus").asText(""));
        assertEquals("UNKNOWN", rec12345678.at("/it3-2/oldStatus").asText(""));
        assertEquals("Decommissioned", rec12345678.at("/it3-1/newStatus").asText(""));
        assertEquals("OnLoan", rec12345678.at("/it3-1/oldStatus").asText(""));

        System.out.println("OK");
    }

    @Test(timeout = 10_000L)
    public void testCompleteHoldingsItemsCreateEmptyJson() throws Exception {
        System.out.println("testCompleteHoldingsItemsCreateEmptyJson");
        jpa(em -> {
            mockUpdateBean(em)
                    .completeHoldingsItemsUpdate(completeReq1());
        });
        getQueue(); // Just for logging
        clearQueue();

        jpa(em -> {
            mockUpdateBean(em)
                    .completeHoldingsItemsUpdate(completeReqEmpty());
        });
        HashMap<String, Set<String>> queue = getQueue();
        assertEquals("queue size", 1, queue.get("complete").size());

        String rec12345678str = queue.get("complete").stream()
                .filter(s -> s.contains("12345678"))
                .findAny().orElseThrow(() -> new RuntimeException("Cannot find 12345678 record"));
        System.out.println("rec12345678 = " + rec12345678str);
        JsonNode rec12345678 = O.readTree(rec12345678str.substring(rec12345678str.indexOf('{')));
        assertEquals("Decommissioned", rec12345678.at("/it3-1/newStatus").asText(""));
        assertEquals("OnLoan", rec12345678.at("/it3-1/oldStatus").asText(""));

        System.out.println("OK");
    }

    /**
     *
     * @throws Exception
     */
    @Test(timeout = 10_000L)
    public void testCompleteDecommissions() throws Exception {
        System.out.println("testCompleteDecommissions");

        jpa(em -> {
            HoldingsItemsUpdateResult respSetup = mockUpdateBean(em)
                    .holdingsItemsUpdate(updateReq1());
            System.out.println("status = " + respSetup.getHoldingsItemsUpdateStatusMessage());
            assertEquals("Expedcted success from request", HoldingsItemsUpdateStatusEnum.OK, respSetup.getHoldingsItemsUpdateStatus());
        });
        jpa(em -> {
            HoldingsItemsUpdateResult resp = mockUpdateBean(em)
                    .completeHoldingsItemsUpdate(completeReq1());
            System.out.println("status = " + resp.getHoldingsItemsUpdateStatusMessage());
            assertEquals("Expedcted success from request", HoldingsItemsUpdateStatusEnum.OK, resp.getHoldingsItemsUpdateStatus());
        });
        jpa(em -> {
            HoldingsItemsUpdateResult respOnline = mockUpdateBean(em)
                    .onlineHoldingsItemsUpdate(onlineReqCreate());
            System.out.println("status = " + respOnline.getHoldingsItemsUpdateStatusMessage());
            assertEquals("Expedcted success from request", HoldingsItemsUpdateStatusEnum.OK, respOnline.getHoldingsItemsUpdateStatus());
        });
        assertEquals("Expected collections", 2, countAllIssues());
        assertEquals("Expected items", 2, countAllItems());
        assertEquals("Expected online", 1, countItems(StatusType.ONLINE));
    }

    @Test(timeout = 10_000L)
    public void testNoteGetsUpdated() throws Exception {
        System.out.println("testNoteGetsUpdated");
        jpa(em -> {
            mockUpdateBean(em).holdingsItemsUpdate(updateReqNote1());
        });
        String noteBefore = checkRow(101010, "12345678", "I1", "it1-1").getOrDefault("b.note", "N/A");
        assertEquals("Original Note", noteBefore);
        jpa(em -> {
            mockUpdateBean(em).holdingsItemsUpdate(updateReqNote2());
        });
        String noteAfter = checkRow(101010, "12345678", "I1", "it1-1").getOrDefault("b.note", "N/A");
        assertEquals("Updated Note", noteAfter);
    }

    @Test(timeout = 10_000L)
    public void testLoanRestrictionUpdate() throws Exception {
        System.out.println("testLoanRestrictionUpdate");

        // Default value
        {
            jpa(em -> {
                HoldingsItem item = item("it1-1", branch, "234567", department, location, subLocation, circulationRule,
                                         StatusType.ON_SHELF, date("2017-01-01"));
                item.setLoanRestriction(null);
                HoldingsItemsUpdateRequest req =
                        holdingsItemsUpdateRequest(101010, null, "track-update-1",
                                                   bibliographicItem("12345678", modified("2017-09-07T09:09:00.000Z"), "Original Note",
                                                                     holding("I1", "Issue #1", date("2199-01-01"), 0, item)));
                mockUpdateBean(em).holdingsItemsUpdate(req);
            });

            jpa(em -> {
                HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
                BibliographicItemEntity notSet = dao.getRecordCollectionUnLocked("12345678", 101010);
                ItemEntity itemNotSet = notSet.stream().findFirst().orElseThrow(() -> new RuntimeException("No issues"))
                        .stream().findFirst().orElseThrow(() -> new RuntimeException("No items"));
                assertEquals(LoanRestriction.EMPTY, itemNotSet.getLoanRestriction());
            });
        }

        // Set to e
        System.out.println("Set loanRestriction to 'e'");
        {
            jpa(em -> {
                HoldingsItem item = item("it1-1", branch, "234567", department, location, subLocation, circulationRule,
                                         StatusType.ON_SHELF, date("2017-01-01"));
                item.setLoanRestriction("e");
                HoldingsItemsUpdateRequest req =
                        holdingsItemsUpdateRequest(101010, null, "track-update-1",
                                                   bibliographicItem("12345678", modified("2017-09-07T09:09:01.000Z"), "Original Note",
                                                                     holding("I1", "Issue #1", date("2199-01-01"), 0, item)));
                mockUpdateBean(em).holdingsItemsUpdate(req);
            });

            jpa(em -> {
                HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
                BibliographicItemEntity setToE = dao.getRecordCollectionUnLocked("12345678", 101010);
                ItemEntity itemSetToX = setToE.stream().findFirst().orElseThrow(() -> new RuntimeException("No issues"))
                        .stream().findFirst().orElseThrow(() -> new RuntimeException("No items"));
                assertEquals(LoanRestriction.e, itemSetToX.getLoanRestriction());
            });
        }

        // Set to null
        System.out.println("Set loanRestriction to unspecified (same as empty)");
        {
            jpa(em -> {
                HoldingsItem item = item("it1-1", branch, "234567", department, location, subLocation, circulationRule,
                                         StatusType.ON_SHELF, date("2017-01-01"));
                item.setLoanRestriction(null);
                HoldingsItemsUpdateRequest req =
                        holdingsItemsUpdateRequest(101010, null, "track-update-1",
                                                   bibliographicItem("12345678", modified("2017-09-07T09:09:02.000Z"), "Original Note",
                                                                     holding("I1", "Issue #1", date("2199-01-01"), 0, item)));
                mockUpdateBean(em).holdingsItemsUpdate(req);
            });

            jpa(em -> {
                HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
                BibliographicItemEntity retain = dao.getRecordCollectionUnLocked("12345678", 101010);
                ItemEntity itemRetain = retain.stream().findFirst().orElseThrow(() -> new RuntimeException("No issues"))
                        .stream().findFirst().orElseThrow(() -> new RuntimeException("No items"));
                assertEquals(LoanRestriction.EMPTY, itemRetain.getLoanRestriction());
                assertEquals(Instant.parse("2017-09-07T09:09:02.000Z"), itemRetain.getModified());
            });
        }
    }

    @Test(timeout = 10_000L)
    public void testDecommissionedInQueueJson() throws Exception {
        System.out.println("testDecommissionedInQueueJson");

        {
            clearQueue();
            jpa(em -> {
                HoldingsItem item = item("it1-1", branch, "234567", department, location, subLocation, circulationRule,
                                         StatusType.ON_SHELF, date("2017-01-01"));
                item.setLoanRestriction(null);
                HoldingsItemsUpdateRequest req =
                        holdingsItemsUpdateRequest(101010, null, "track-update-1",
                                                   bibliographicItem("12345678", modified("2017-09-07T09:09:00.000Z"), "Original Note",
                                                                     holding("I1", "Issue #1", date("2199-01-01"), 0, item)));
                mockUpdateBean(em).holdingsItemsUpdate(req);
            });

            HashMap<String, Set<String>> queue = getQueue();
            assertTrue(queue.containsKey("update"));
            assertEquals(1, queue.get("update").size());
            String queued = queue.get("update").iterator().next();
            String json = queued.substring(queued.lastIndexOf('|') + 1);
            assertThat(json, containsString("\"newStatus\":\"OnShelf\""));
            assertThat(json, containsString("\"oldStatus\":\"UNKNOWN\""));
        }
        {
            clearQueue();
            jpa(em -> {
                HoldingsItem item = item("it1-1", branch, "234567", department, location, subLocation, circulationRule,
                                         StatusType.DECOMMISSIONED, date("2017-01-01"));
                item.setLoanRestriction(null);
                HoldingsItemsUpdateRequest req =
                        holdingsItemsUpdateRequest(101010, null, "track-update-1",
                                                   bibliographicItem("12345678", modified("2017-09-07T09:09:00.001Z"), "Original Note",
                                                                     holding("I1", "Issue #1", date("2199-01-01"), 0, item)));
                mockUpdateBean(em).holdingsItemsUpdate(req);
            });

            HashMap<String, Set<String>> queue = getQueue();
            assertTrue(queue.containsKey("update"));
            assertEquals(1, queue.get("update").size());
            String queued = queue.get("update").iterator().next();
            String json = queued.substring(queued.lastIndexOf('|') + 1);
            assertThat(json, containsString("\"newStatus\":\"Decommissioned\""));
            assertThat(json, containsString("\"oldStatus\":\"OnShelf\""));
        }
    }

    public HoldingsItemsUpdateRequest updateReq1() throws DatatypeConfigurationException {
        return holdingsItemsUpdateRequest(
                101010, null, "track-update-1",
                bibliographicItem(
                        "12345678", modified("2017-09-07T09:09:00.000Z"), "Some Note",
                        holding("I1", "Issue #1", date("2199-01-01"), 0,
                                item("it1-1", branch, "234567", department, location, subLocation, circulationRule,
                                     StatusType.ON_SHELF, date("2017-01-01")),
                                item("it1-2", branch, "234567", department, location, subLocation, circulationRule,
                                     StatusType.ON_SHELF, date("2017-01-01"))),
                        holding("I2", "Issue #2", null, 1,
                                item("it2-1", branch, "234567", department, location, subLocation, circulationRule,
                                     StatusType.ON_ORDER, date("2017-01-01")))
                ));
    }

    public HoldingsItemsUpdateRequest updateReq2() throws DatatypeConfigurationException {
        return holdingsItemsUpdateRequest(
                101010, null, "track-update-2",
                bibliographicItem(
                        "12345678", modified("2017-09-07T09:09:00.001Z"), "Some Note",
                        holding("I1", "Issue #1", date("2199-01-01"), 0,
                                item("it1-1", branch, "234567", department, location, subLocation, circulationRule,
                                     StatusType.ON_SHELF, date("2017-01-01")),
                                item("it1-2", branch, "234567", department, location, subLocation, circulationRule,
                                     StatusType.ON_LOAN, date("2017-01-01"))), holding("I2", "Issue #2", null, 1,
                                                                                       item("it2-1", branch, "234567", department, location, subLocation, circulationRule,
                                                                                            StatusType.ON_ORDER, date("2017-01-01")))),
                bibliographicItem(
                        "87654321", modified("2017-09-07T09:09:00.001Z"), "Some Note",
                        holding("I1", "Issue #1", date("2199-01-01"), 0,
                                item("it3-1", branch, "234567", department, location, subLocation, circulationRule,
                                     StatusType.ON_LOAN, date("2017-01-01")))) // new
        );
    }

    private OnlineHoldingsItemsUpdateRequest onlineReqCreate() throws DatatypeConfigurationException {
        return onlineHoldingsItemsUpdateRequest(
                101010, null, "track-online-1",
                onlineBibliographicItem("12345678", modified("2017-09-07T09:10:00.765Z"), true));
    }

    private OnlineHoldingsItemsUpdateRequest onlineReqDelete() throws DatatypeConfigurationException {
        return onlineHoldingsItemsUpdateRequest(
                101010, null, "track-online-2",
                onlineBibliographicItem("12345678", modified("2017-09-07T09:10:21.765Z"), false));
    }

    private CompleteHoldingsItemsUpdateRequest completeReq1() throws DatatypeConfigurationException {
        return completeHoldingsItemsUpdateRequest(
                101010, null, "track-complete-1",
                completeBibliographicItem(
                        "12345678", modified("2017-09-07T09:09:01.001Z"), "Other Note",
                        holding("I3", "Issue #3", null, 1,
                                item("it3-1", branch, "234567", department, location, subLocation, circulationRule,
                                     StatusType.ON_LOAN, date("2017-01-01")))));
    }

    private CompleteHoldingsItemsUpdateRequest completeReq2() throws DatatypeConfigurationException {
        return completeHoldingsItemsUpdateRequest(
                101010, null, "track-complete-1",
                completeBibliographicItem(
                        "12345678", modified("2017-09-07T09:09:02.001Z"), "Other Note",
                        holding("I3", "Issue #3", null, 1,
                                item("it3-1", branch, "234567", department, location, subLocation, circulationRule,
                                     StatusType.ON_LOAN, date("2017-01-01"))),
                        holding("I4", "Issue #4", null, 1,
                                item("it4-1", branch, "234567", department, location, subLocation, circulationRule,
                                     StatusType.ON_LOAN, date("2017-01-01")))));
    }

    private CompleteHoldingsItemsUpdateRequest completeReq3() throws DatatypeConfigurationException {
        return completeHoldingsItemsUpdateRequest(
                101010, null, "track-complete-1",
                completeBibliographicItem(
                        "12345678", modified("2017-09-07T09:09:03.001Z"), "Other Note",
                        holding("I3", "Issue #3", null, 1,
                                item("it3-2", branch, "234567", department, location, subLocation, circulationRule,
                                     StatusType.ON_LOAN, date("2017-01-01"))),
                        holding("I3", "Issue #3", null, 1,
                                item("it3-3", branch, "234567", department, location, subLocation, circulationRule,
                                     StatusType.ON_LOAN, date("2017-01-01")))));
    }

    private CompleteHoldingsItemsUpdateRequest completeReqEmpty() throws DatatypeConfigurationException {
        return completeHoldingsItemsUpdateRequest(
                101010, null, "track-complete-empty", completeBibliographicItem(
                        "12345678", modified("2017-09-07T09:09:04.001Z"), "Other Note"));

    }

    public HoldingsItemsUpdateRequest updateReqNote1() throws DatatypeConfigurationException {
        return holdingsItemsUpdateRequest(
                101010, null, "track-update-1",
                bibliographicItem(
                        "12345678", modified("2017-09-07T09:09:00.000Z"), "Original Note",
                        holding("I1", "Issue #1", date("2199-01-01"), 0,
                                item("it1-1", branch, "234567", department, location, subLocation, circulationRule,
                                     StatusType.ON_SHELF, date("2017-01-01")))
                ));
    }

    public HoldingsItemsUpdateRequest updateReqNote2() throws DatatypeConfigurationException {
        return holdingsItemsUpdateRequest(
                101010, null, "track-update-2",
                bibliographicItem(
                        "12345678", modified("2017-09-07T09:09:00.100Z"), "Updated Note",
                        holding("I2", "Issue #2", date("2199-01-01"), 0,
                                item("it2-1", branch, "234567", department, location, subLocation, circulationRule,
                                     StatusType.ON_SHELF, date("2017-01-01")))
                ));
    }

    private UpdateBean mockUpdateBean(EntityManager em) throws SQLException {
        UpdateBean mock = mock(UpdateBean.class);
        mock.config = mockConfig();
        mock.em = em;
        mock.requestCounter = mock(Counter.class);
        mock.requestUpdateCounter = mock(Counter.class);
        mock.requestCompleteCounter = mock(Counter.class);
        mock.requestOnlineCounter = mock(Counter.class);
        mock.requestInvalidCounter = mock(Counter.class);
        mock.requestSystemErrorCounter = mock(Counter.class);
        mock.requestAuthenticationErrorCounter = mock(Counter.class);
        mock.saveCollectionTimer = mock(Timer.class);
        mock.loadCollectionTimer = mock(Timer.class);
        AccessValidator validator = mock(AccessValidator.class);
        mock.validator = validator;
        when(validator.validate(any(Authentication.class)))
                .then((invocation) -> {
                    Authentication auth = (Authentication) invocation.getArguments()[0];
                    if (auth == null)
                        return null;
                    return auth.getGroupIdAut();
                });

        doCallRealMethod().when(mock).setWebServiceContext(any(WebServiceContext.class));
        doCallRealMethod().when(mock).holdingsItemsUpdate(any(HoldingsItemsUpdateRequest.class));
        doCallRealMethod().when(mock).completeHoldingsItemsUpdate(any(CompleteHoldingsItemsUpdateRequest.class));
        doCallRealMethod().when(mock).onlineHoldingsItemsUpdate(any(OnlineHoldingsItemsUpdateRequest.class));

        WebServiceContext wsc = mock(WebServiceContext.class);
        doReturn(mockMessageContext()).when(wsc).getMessageContext();
        mock.setWebServiceContext(wsc);

        return mock;
    }

    private void clearQueue() throws SQLException {
        try (Connection connection = PG.createConnection() ;
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("TRUNCATE queue");
        }
    }

    private HashMap<String, Set<String>> getQueue() throws SQLException {
        HashMap<String, Set<String>> result = new HashMap<>();
        try (Connection connection = PG.createConnection() ;
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
        return new Config("DISABLE_AUTHENTICATION=true",
                          "IDP_URL=ANY",
                          "IDP_RIGHTS=common,any");
    }

    private HoldingsItemsUpdateRequest holdingsItemsUpdateRequest(int agencyId, Authentication authentication, String trackingId, BibliographicItem... bibliographicItems) {
        HoldingsItemsUpdateRequest req = new HoldingsItemsUpdateRequest();
        req.setAgencyId(agencyId);
        req.setAuthentication(authentication);
        req.setTrackingId(trackingId);
        req.getBibliographicItems().addAll(Arrays.asList(bibliographicItems));
        return req;
    }

    private CompleteHoldingsItemsUpdateRequest completeHoldingsItemsUpdateRequest(int agencyId, Authentication authentication, String trackingId, CompleteBibliographicItem bibliographicItem) {
        CompleteHoldingsItemsUpdateRequest req = new CompleteHoldingsItemsUpdateRequest();
        req.setAgencyId(agencyId);
        req.setAuthentication(authentication);
        req.setTrackingId(trackingId);
        req.setCompleteBibliographicItem(bibliographicItem);
        return req;
    }

    private OnlineHoldingsItemsUpdateRequest onlineHoldingsItemsUpdateRequest(int agencyId, Authentication authentication, String trackingId, OnlineBibliographicItem... bibliographicItems) {
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

    private HoldingsItem item(String itemId, String branch, String branchId, String department, String location, String subLocation, String circulationRule, StatusType status, XMLGregorianCalendar accessionDate) {
        HoldingsItem item = new HoldingsItem();
        item.setItemId(itemId);
        item.setBranch(branch);
        item.setBranchId(branchId);
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

    private int countAllIssues() throws SQLException {
        try (Connection db = PG.createConnection() ;
             PreparedStatement stmt = db.prepareStatement("SELECT COUNT(*) FROM issue") ;
             ResultSet resultSet = stmt.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return -1;
    }

    private int countAllItems() throws SQLException {
        try (Connection db = PG.createConnection() ;
             PreparedStatement stmt = db.prepareStatement("SELECT COUNT(*) FROM item") ;
             ResultSet resultSet = stmt.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return -1;
    }

    private int countItems(StatusType type) throws SQLException {
        try (Connection db = PG.createConnection() ;
             PreparedStatement stmt = db.prepareStatement("SELECT COUNT(*) FROM item WHERE status=?")) {
            stmt.setString(1, type.value());
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return -1;
    }

    private HashMap<String, String> checkRow(int agencyId, String bibliographicRecordId, String issueId, String itemId) throws SQLException {
        try (Connection db = PG.createConnection() ;
             PreparedStatement stmt = db.prepareStatement(
                     "SELECT " +
                     "c.agencyid, " +
                     "c.bibliographicrecordid, " +
                     "c.issueid, " +
                     "c.issuetext, " +
                     "c.expecteddelivery, " +
                     "c.readyforloan, " +
                     "b.note, " +
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
                     "FROM issue AS c JOIN item AS i USING (" +
                     "agencyid, " +
                     "bibliographicrecordid, " +
                     "issueid" +
                     ") JOIN bibliographicItem AS b USING(" +
                     "agencyid, " +
                     "bibliographicrecordid" +
                     ") WHERE " +
                     "agencyid=? AND " +
                     "bibliographicrecordid=? AND " +
                     "issueid=? AND " +
                     "itemid=? ")) {
            stmt.setInt(1, agencyId);
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
                    setString("b.note", resultSet, i, row);
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
                .toString());
    }
}
