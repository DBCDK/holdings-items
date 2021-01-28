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

import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.ItemKey;
import dk.dbc.holdingsitems.jpa.Status;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class HoldingsItemsDAOIT extends JpaBase {

    @Test
    public void testEnqueue() throws Exception {
        System.out.println("testEnqueue");
        jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, "FOO");
            dao.enqueue("12345678", 888888, "{}", "worker");
            dao.enqueue("87654321", 888888, "{}", "worker", 1000);
        });
        flushAndEvict();
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
            System.out.println("results = " + results);
            assertEquals(2, results.size());
            assertTrue(results.contains("12345678|888888|FOO"));
            assertTrue(results.contains("87654321|888888|FOO"));
        }
    }

    @Test(timeout = 2_000L)
    public void allLiveBibliographicIdsForAgency() throws Exception {
        System.out.println("allLiveBibliographicIdsForAgency");

        jpa(em -> {
            make4(em);
        });

        flushAndEvict();

        Set<String> bibIds = jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            return dao.getBibliographicIds(870970);
        });
        System.out.println("bibIds = " + bibIds);
        assertThat(bibIds, hasItems("25912233", "abc"));

        // Decommission abc:i1:d
        jpa(em -> {
            BibliographicItemEntity b = BibliographicItemEntity.from(em, 870970, "abc", Instant.now(), LocalDate.MAX);
            IssueEntity c = b.issue("i1", Instant.MIN);
            c.item("d", Instant.MIN)
                    .setStatus(Status.DECOMMISSIONED);
            em.merge(b);
        });

        flushAndEvict();

        List<String> bibIdsDecom = jpa(em -> {
            return em.createQuery("SELECT h.bibliographicRecordId" +
                                  " FROM ItemEntity h" +
                                  " WHERE h.agencyId = :agencyId" +
                                  "  AND h.status != :status" +
                                  " GROUP BY h.agencyId, h.bibliographicRecordId",
                                  String.class)
                    .setParameter("agencyId", 870970)
                    .setParameter("status", Status.DECOMMISSIONED)
                    .getResultList();
        });
        System.out.println("bibIds = " + bibIdsDecom);
        assertThat(bibIdsDecom, hasItems("25912233"));
    }

    @Test(timeout = 2_000L)
    public void allIssuesForAgencyAndBibliographicRecordId() throws Exception {
        System.out.println("allIssuesForAgencyAndBibliographicRecordId");
        jpa(em -> {
            make4(em);
        });

        flushAndEvict();
        Set<String> issues = jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            return dao.getIssueIds("25912233", 870970);
        });
        System.out.println("issues = " + issues);
        assertThat(issues, hasItems("i1", "i2"));
    }

    @Test(timeout = 2_000L)
    public void statusMapForAgencyAndBibliographicRecordId() throws Exception {
        System.out.println("statusMapForAgencyAndBibliographicRecordId");
        jpa(em -> {
            BibliographicItemEntity b1 = BibliographicItemEntity.from(em, 870970, "25912233", Instant.MIN, LocalDate.now());
            fill(b1);

            IssueEntity c1 = fill(b1.issue("i1", Instant.MIN));
            fill(c1.item("a", Instant.MIN))
                    .setStatus(Status.ON_LOAN);
            fill(c1.item("b", Instant.MIN))
                    .setStatus(Status.ON_LOAN);
            fill(c1.item("c", Instant.MIN))
                    .setStatus(Status.ON_SHELF);
            IssueEntity c2 = fill(b1.issue("i2", Instant.MIN));
            fill(c2.item("d", Instant.MIN))
                    .setStatus(Status.ON_ORDER);
            fill(c2.item("e", Instant.MIN))
                    .setStatus(Status.ON_SHELF);
            b1.save();
        });

        flushAndEvict();

        Map<Status, Long> statusMap = jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            return dao.getStatusFor("25912233", 870970);
        });
        System.out.println("statusMap = " + statusMap);

        assertThat(statusMap.keySet(), hasItems(Status.ON_LOAN,
                                                Status.ON_SHELF,
                                                Status.ON_ORDER));
        assertThat(statusMap.get(Status.ON_LOAN), is(2L));
        assertThat(statusMap.get(Status.ON_SHELF), is(2L));
        assertThat(statusMap.get(Status.ON_ORDER), is(1L));
        assertThat(statusMap.size(), is(3));
    }

    @Test(timeout = 2_000L)
    public void hasLiveHoldingsForAgencyAndBibliographicRecordId() throws Exception {
        System.out.println("hasLiveHoldingsForAgencyAndBibliographicRecordId");
        // Create live holding
        jpa(em -> {
            BibliographicItemEntity b1 = BibliographicItemEntity.from(em, 870970, "25912233", Instant.MIN, LocalDate.now());
            fill(b1);

            IssueEntity c1 = fill(b1.issue("i1", Instant.MIN));
            fill(c1.item("a", Instant.MIN))
                    .setStatus(Status.ON_SHELF);
            b1.save();
        });

        boolean hasLiveHoldings = jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            return dao.hasLiveHoldings("25912233", 870970);
        });
        assertTrue(hasLiveHoldings);

        // set to decommissioned
        jpa(em -> {
            BibliographicItemEntity b1 = BibliographicItemEntity.from(em, 870970, "25912233", Instant.MIN, LocalDate.now());
            IssueEntity c1 = b1.issue("i1", Instant.MIN);
            c1.item("a", Instant.MIN)
                    .setStatus(Status.DECOMMISSIONED);
            b1.save();
        });

        hasLiveHoldings = jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            return dao.hasLiveHoldings("25912233", 870970);
        });
        assertFalse(hasLiveHoldings);

        // Create other issue with live
        jpa(em -> {
            BibliographicItemEntity b1 = BibliographicItemEntity.from(em, 870970, "25912233", Instant.MIN, LocalDate.now());
            IssueEntity c2 = fill(b1.issue("i2", Instant.MIN));
            fill(c2.item("b", Instant.MIN))
                    .setStatus(Status.ON_SHELF);
            c2.save();
        });

        hasLiveHoldings = jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            return dao.hasLiveHoldings("25912233", 870970);
        });
        assertTrue(hasLiveHoldings);
    }

    @Test(timeout = 2_000L)
    public void accessByBibliographicItem() throws Exception {
        System.out.println("accessByBibliographicItem");
        jpa(em -> {

            BibliographicItemEntity b1 = BibliographicItemEntity.from(em, 870970, "25912233", Instant.MIN, LocalDate.now());
            fill(b1);

            IssueEntity c1 = fill(b1.issue("i1", Instant.MIN));
            fill(c1.item("a", Instant.MIN))
                    .setStatus(Status.ON_SHELF);

            ItemEntity i1 = c1.item("it1", Instant.MIN);
            fill(i1);
            b1.save();

        });
        flushAndEvict();

        ItemEntity item = jpa(em -> {
            return em.find(ItemEntity.class, new ItemKey(870970, "25912233", "i1", "it1"));
        });
        System.out.println("item = " + item);
        assertThat(item, notNullValue());
    }

//  _   _      _                   _____                 _   _
// | | | | ___| |_ __   ___ _ __  |  ___|   _ _ __   ___| |_(_) ___  _ __  ___
// | |_| |/ _ \ | '_ \ / _ \ '__| | |_ | | | | '_ \ / __| __| |/ _ \| '_ \/ __|
// |  _  |  __/ | |_) |  __/ |    |  _|| |_| | | | | (__| |_| | (_) | | | \__ \
// |_| |_|\___|_| .__/ \___|_|    |_|   \__,_|_| |_|\___|\__|_|\___/|_| |_|___/
//              |_|
    private void make4(EntityManager em) {

        BibliographicItemEntity b1 = BibliographicItemEntity.from(em, 870970, "25912233", Instant.MIN, LocalDate.now());
        fill(b1);

        IssueEntity c1 = b1.issue("i1", Instant.MIN);
        fill(c1);
        ItemEntity i1 = c1.item("a", Instant.MIN);
        fill(i1);

        IssueEntity c2 = b1.issue("i2", Instant.MIN);
        fill(c2);
        ItemEntity i2 = c2.item("b", Instant.MIN);
        fill(i2);
        b1.save();

        BibliographicItemEntity b2 = BibliographicItemEntity.from(em, 123456, "25912233", Instant.MIN, LocalDate.now());
        fill(b2);

        IssueEntity c3 = b2.issue("i1", Instant.MIN);
        fill(c3);
        ItemEntity i3 = c3.item("c", Instant.MIN);
        fill(i3);
        b2.save();

        BibliographicItemEntity b3 = BibliographicItemEntity.from(em, 870970, "abc", Instant.MIN, LocalDate.now());
        fill(b3);

        IssueEntity c4 = b3.issue("i1", Instant.MIN);
        fill(c4);
        ItemEntity i4 = c4.item("d", Instant.MIN);
        fill(i4);
        b3.save();
    }

}
