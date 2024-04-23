package dk.dbc.holdingsitems.content.api.v1.update;

import dk.dbc.holdingsitems.content.JpaBase;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.oss.ns.holdingsitemsupdate.BibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteBibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteHoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.Holding;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItem;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.ModificationTimeStamp;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineBibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineHoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.StatusType;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class UpdateV1Test extends JpaBase {

    @Before
    public void setupQueueRules() throws SQLException {
        try (Connection connection = PG.createConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO queue_rules(supplier, consumer) VALUES('COMPLETE', 'complete')");
            stmt.execute("INSERT INTO queue_rules(supplier, consumer) VALUES('ONLINE', 'online')");
            stmt.execute("INSERT INTO queue_rules(supplier, consumer) VALUES('UPDATE', 'update')");
        }
    }

    @After
    public void cleanQueueRules() throws SQLException {
        try (Connection connection = PG.createConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE queue_rules");
        }
    }

    @Test(timeout = 2_000L)
    public void testComplete() throws Exception {
        System.out.println("testComplete");
        jpa(em -> {
            bean(em).completeHoldingsItemsUpdate(new CompleteHoldingsItemsUpdateRequest()
                    .useAgencyId(700000)
                    .useAuthentication(null)
                    .useTrackingId("fool")
                    .useCompleteBibliographicItem(complete("a")
                            .useHolding(new Holding()
                                    .useExpectedDeliveryDate(Instant.now().plus(5, DAYS))
                                    .useIssueId("")
                                    .useIssueText("")
                                    .useReadyForLoan(0L)
                                    .useHoldingsItem(holdingsItem("i1")
                                            .useStatus(StatusType.ON_ORDER)))));
        });
        testItems(700000, "a", "a//i1");
        testQueue("complete/700000/a");

        jpa(em -> {
            bean(em).completeHoldingsItemsUpdate(new CompleteHoldingsItemsUpdateRequest()
                    .useAgencyId(700000)
                    .useAuthentication(null)
                    .useTrackingId("fool")
                    .useCompleteBibliographicItem(complete("a")
                            .useHolding(new Holding()
                                    .useExpectedDeliveryDate(Instant.now().plus(5, DAYS))
                                    .useIssueId("1")
                                    .useIssueText("Volume 1")
                                    .useReadyForLoan(0L)
                                    .useHoldingsItem(holdingsItem("i1")
                                            .useStatus(StatusType.ON_SHELF)))));
        });
        testItems(700000, "a", "a/1/i1");
        testQueue("complete/700000/a");
    }

    @Test(timeout = 2_000L)
    public void testCompleteKeepsOnlineAndUpdated() throws Exception {
        System.out.println("testCompleteKeepsOnlineAndUpdated");
        jpa(em -> {
            bean(em).completeHoldingsItemsUpdate(new CompleteHoldingsItemsUpdateRequest()
                    .useAgencyId(700000)
                    .useAuthentication(null)
                    .useTrackingId("fool")
                    .useCompleteBibliographicItem(complete("a")
                            .useHolding(new Holding()
                                    .useExpectedDeliveryDate(Instant.now().plus(5, DAYS))
                                    .useIssueId("1")
                                    .useIssueText("Volume 1")
                                    .useReadyForLoan(0L)
                                    .useHoldingsItem(holdingsItem("i1")
                                            .useStatus(StatusType.ON_SHELF)))));
        });
        testItems(700000, "a", "a/1/i1");
        testQueue("complete/700000/a");

        // Update one item much later
        jpa(em -> {
            bean(em).holdingsItemsUpdate(new HoldingsItemsUpdateRequest()
                    .useAgencyId(700000)
                    .useAuthentication(null)
                    .useTrackingId("fool")
                    .useBibliographicItem(bibliographic("a")
                            .useModificationTimeStamp(ts(Instant.now().plus(1, DAYS)))
                            .useHolding(new Holding()
                                    .useExpectedDeliveryDate(Instant.now().plus(5, DAYS))
                                    .useIssueId("1")
                                    .useIssueText("Volume 1")
                                    .useReadyForLoan(0L)
                                    .useHoldingsItem(holdingsItem("i2")
                                            .useStatus(StatusType.ON_SHELF)))));
        });
        testItems(700000, "a", "a/1/i1", "a/1/i2");
        testQueue("update/700000/a");

        // Add online
        jpa(em -> {
            bean(em).onlineHoldingsItemsUpdate(new OnlineHoldingsItemsUpdateRequest()
                    .useAgencyId(700000)
                    .useAuthentication(null)
                    .useTrackingId("fool")
                    .useOnlineBibliographicItem(online("a")
                            .useHasOnlineHolding(true)));
        });
        testItems(700000, "a", "a/1/i1", "a/1/i2", "a//");
        testQueue("online/700000/a");

        // move i1 to issue #2 - online should be kept, i2 is modified in the future relative to this request, should be kept
        jpa(em -> {
            bean(em).completeHoldingsItemsUpdate(new CompleteHoldingsItemsUpdateRequest()
                    .useAgencyId(700000)
                    .useAuthentication(null)
                    .useTrackingId("fool")
                    .useCompleteBibliographicItem(complete("a")
                            .useHolding(new Holding()
                                    .useExpectedDeliveryDate(Instant.now().plus(5, DAYS))
                                    .useIssueId("2")
                                    .useIssueText("Volume 2")
                                    .useReadyForLoan(0L)
                                    .useHoldingsItem(holdingsItem("i1")
                                            .useStatus(StatusType.ON_SHELF)))));
        });
        testItems(700000, "a", "a/2/i1", "a/1/i2", "a//");
        testQueue("complete/700000/a");


        // move item from one issue to another
        jpa(em -> {
            bean(em).holdingsItemsUpdate(new HoldingsItemsUpdateRequest()
                    .useAgencyId(700000)
                    .useAuthentication(null)
                    .useTrackingId("fool")
                    .useBibliographicItem(bibliographic("a")
                            .useModificationTimeStamp(ts(Instant.now().plus(1, DAYS)))
                            .useHolding(new Holding()
                                    .useExpectedDeliveryDate(Instant.now().plus(5, DAYS))
                                    .useIssueId("x")
                                    .useIssueText("Volume 1")
                                    .useReadyForLoan(0L)
                                    .useHoldingsItem(holdingsItem("i2")
                                            .useStatus(StatusType.ON_SHELF)))));
        });
        testItems(700000, "a", "a/2/i1", "a/x/i2", "a//");
        testQueue("update/700000/a");
    }

    private CompleteBibliographicItem complete(String bibliographicRecordId) {
        return new CompleteBibliographicItem()
                .useBibliographicRecordId(bibliographicRecordId)
                .useModificationTimeStamp(ts(Instant.now()))
                .useNote("[note]");
    }

    private BibliographicItem bibliographic(String bibliographicRecordId) {
        return new BibliographicItem()
                .useBibliographicRecordId(bibliographicRecordId)
                .useModificationTimeStamp(ts(Instant.now()))
                .useNote("[note]");
    }

    private OnlineBibliographicItem online(String bibliographicRecordId) {
        return new OnlineBibliographicItem()
                .useBibliographicRecordId(bibliographicRecordId)
                .useModificationTimeStamp(ts(Instant.now()));
    }

    private HoldingsItem holdingsItem(String itemId) {
        return new HoldingsItem()
                .useAccessionDate(Instant.now().minus(4, DAYS))
                .useBranch("br")
                .useBranchId("700000")
                .useCirculationRule("cr")
                .useDepartment("dep")
                .useItemId(itemId)
                .useLastLoanDate(Instant.EPOCH)
                .useLoanRestriction(null)
                .useLocation("loc")
                .useStatus(StatusType.ON_SHELF)
                .useSubLocation("sl");
    }

    private void testQueue(String... queueItems) throws SQLException {
        HashSet<String> rows = new HashSet<>();
        try (Connection connection = PG.createConnection();
             Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery("DELETE FROM queue RETURNING consumer || '/' || agencyid || '/' || bibliographicrecordid")) {
            while (resultSet.next()) {
                rows.add(resultSet.getString(1));
            }
        }
        assertThat(rows, containsInAnyOrder(queueItems));
    }

    private void testItems(int agencyId, String bibliographicRecordId, String... itemIds) {
        jpa(em -> {
            BibliographicItemEntity entity = BibliographicItemEntity.fromUnLocked(em, agencyId, bibliographicRecordId);
            Set<String> collect = entity.stream().flatMap(IssueEntity::stream).map(itemId()).collect(Collectors.toSet());
            assertThat(collect, containsInAnyOrder(itemIds));
        });
    }

    private UpdateV1 bean(EntityManager em) {
        UpdateV1 bean = new UpdateV1();
        bean.accessValidator = new AccessValidator();
        bean.accessValidator.disableAuthentication = true;
        bean.updateLogic = new UpdateV1Logic();
        bean.updateLogic.em = em;
        bean.updateLogic.completeOriginalSupplier = "COMPLETE_ORIGINAL";
        bean.updateLogic.completeSupplier = "COMPLETE";
        bean.updateLogic.onlineOriginalSupplier = "ONLINE_ORIGINAL";
        bean.updateLogic.onlineSupplier = "ONLINE";
        bean.updateLogic.updateOriginalSupplier = "UPDATE_ORIGINAL";
        bean.updateLogic.updateSupplier = "UPDATE";
        return bean;
    }

    private ModificationTimeStamp ts(Instant i) {
        return new ModificationTimeStamp()
                .useModificationDateTime(i)
                .useModificationMilliSeconds(0);
    }

    private static Function<ItemEntity, String> itemId() {
        return entity -> entity.getBibliographicRecordId() + "/" + entity.getIssueId() + "/" + entity.getItemId();
    }
}
