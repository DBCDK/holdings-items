package dk.dbc.holdingsitems.content;

import dk.dbc.holdingsitems.content.request.SupersedesRequest;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.LoanRestriction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import javax.persistence.EntityManager;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class SupersedesTest extends JpaBase {

    @Test(timeout = 2_000L)
    public void testSupersedesPutDelete() throws Exception {
        System.out.println("testSupersedesPutDelete");

        jpa(em -> {
            Supersedes supersedes = bean(em);
            SupersedesRequest req = new SupersedesRequest();
            req.supersedes = Arrays.asList("A");
            supersedes.put(req, "B", "t1");
        });

        jpa(em -> {
            Supersedes supersedes = bean(em);
            SupersedesRequest resp = (SupersedesRequest) supersedes.get("B", "t2").getEntity();
            assertThat(resp.supersedes, containsInAnyOrder("A"));
        });

        jpa(em -> {
            Supersedes supersedes = bean(em);
            supersedes.delete("B", "t2");
        });

        jpa(em -> {
            Supersedes supersedes = bean(em);
            int status = supersedes.get("B", "t2").getStatus();
            assertThat(status, is(404));
        });
    }

    @Test(timeout = 2_000L)
    public void testSupersedesPutPut() throws Exception {
        System.out.println("testSupersedesPutPut");
        jpa(em -> {
            Supersedes supersedes = bean(em);
            SupersedesRequest req = new SupersedesRequest();
            req.supersedes = Arrays.asList("A");
            supersedes.put(req, "B", "t1");
        });

        jpa(em -> {
            Supersedes supersedes = bean(em);
            SupersedesRequest req = new SupersedesRequest();
            req.supersedes = Arrays.asList("A", "B");
            supersedes.put(req, "C", "t1");
        });

        jpa(em -> {
            Supersedes supersedes = bean(em);
            assertThat(supersedes.get("B", "t2").getStatus(), is(404));
            SupersedesRequest resp = (SupersedesRequest) supersedes.get("C", "t2").getEntity();
            assertThat(resp.supersedes, containsInAnyOrder("A", "B"));
        });

        jpa(em -> {
            Supersedes supersedes = bean(em);
            SupersedesRequest req = new SupersedesRequest();
            req.supersedes = Arrays.asList("A");
            supersedes.put(req, "B", "t1");
        });

        jpa(em -> {
            Supersedes supersedes = bean(em);
            assertThat(supersedes.get("B", "t2").getStatus(), is(404));
            SupersedesRequest resp = (SupersedesRequest) supersedes.get("C", "t2").getEntity();
            assertThat(resp.supersedes, containsInAnyOrder("A", "B"));
        });
    }

    @Test(timeout = 2_000L)
    public void testQueue() throws Exception {
        System.out.println("testQueue");

        jpa(em -> {
            makeRecord(em, 100000, "A").save();
            makeRecord(em, 200000, "B").save();
            makeRecord(em, 300000, "C").save();
        });

        jpa(em -> {
            Supersedes supersedes = bean(em);
            SupersedesRequest req = new SupersedesRequest();
            req.supersedes = Arrays.asList("A");
            supersedes.put(req, "B", "t1");
        });

        assertThat(queued(), containsInAnyOrder("100000:A", "100000:B",
                                                "200000:A", "200000:B"));

        jpa(em -> {
            Supersedes supersedes = bean(em);
            SupersedesRequest req = new SupersedesRequest();
            req.supersedes = Arrays.asList("A", "B");
            supersedes.put(req, "C", "t1");
        });

        assertThat(queued(), containsInAnyOrder("100000:A", "100000:B", "100000:C",
                                                "200000:A", "200000:B", "200000:C",
                                                "300000:A", "300000:B", "300000:C"));

    }

    private BibliographicItemEntity makeRecord(EntityManager em, int agencyId, String faust) {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now();
        BibliographicItemEntity bib = BibliographicItemEntity.from(em, agencyId, faust, now, today);
        bib.setTrackingId("x");
        IssueEntity issue = bib.issue("i-1", now);
        issue.setExpectedDelivery(today);
        issue.setFirstAccessionDate(today);
        issue.setReadyForLoan(1);
        issue.setIssueText("");
        ItemEntity item = issue.item("it-1", now);
        item.setAccessionDate(today);
        item.setBranch("bran");
        item.setBranchId("" + ( agencyId + 1 ));
        item.setCirculationRule("None");
        item.setDepartment("dep");
        item.setLoanRestriction(LoanRestriction.EMPTY);
        item.setLocation("loc");
        item.setStatus(dk.dbc.holdingsitems.jpa.Status.ON_SHELF);
        item.setSubLocation("sub-loc");
        item.setTrackingId("x");
        return bib;
    }

    private HashSet<String> queued() throws SQLException {
        HashSet<String> queued = new HashSet<>();
        try (Connection connection = PG.createConnection() ;
             Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("DELETE FROM queue RETURNING agencyid, bibliographicrecordid")) {
            while (resultSet.next()) {
                int row = 0;
                int agency = resultSet.getInt(++row);
                String bib = resultSet.getString(++row);
                queued.add(agency + ":" + bib);
            }
        }
        return queued;
    }

    private Supersedes bean(EntityManager em) {
        Supersedes supersedes = new Supersedes();
        supersedes.em = em;
        supersedes.supplier = "SUPERSEDE";
        return supersedes;
    }
}
