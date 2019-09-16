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

import dk.dbc.holdingsitems.jpa.HoldingsItemsCollectionEntity;
import dk.dbc.holdingsitems.jpa.HoldingsItemsItemEntity;
import dk.dbc.holdingsitems.jpa.HoldingsItemsStatus;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class HoldingsItemsDAOIT extends JpaBase {

    @Test
    public void testOldQueue() throws HoldingsItemsException, SQLException {
        System.out.println("testOldQueue");
        jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            dao.enqueueOld("987654321", 870970, "solrIndex1");
        });
        flushAndEvict();
        jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            List<QueueJob> queue = queue();
            System.out.println("queue = " + queue);
            assertEquals("Sizeof queue is 1", 1, queue.size());
            assertEquals("Queue Entry 1 starts with: ", "solrIndex1", queue.get(0).getWorker());
            Instant queued = queue.get(0).getQueued();
            long diff = Math.abs(queued.toEpochMilli() - Instant.now().toEpochMilli());
            System.out.println("diff = " + diff);
            assertTrue("Not too long ago it has been queued", diff < 500);
        });
    }

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
            HoldingsItemsCollectionEntity c = HoldingsItemsCollectionEntity.from(em, 870970, "abc", "i1", Instant.MIN);
            c.item("d", Instant.MIN)
                    .setStatus(HoldingsItemsStatus.DECOMMISSIONED);
            em.merge(c);
        });

        flushAndEvict();

        List<String> bibIdsDecom = jpa(em -> {
            return em.createQuery("SELECT h.bibliographicRecordId" + " FROM HoldingsItemsItemEntity h" + " WHERE h.agencyId = :agencyId" + "  AND h.status != :status" + " GROUP BY h.agencyId, h.bibliographicRecordId", String.class).setParameter("agencyId", 870970).setParameter("status", HoldingsItemsStatus.DECOMMISSIONED).getResultList();
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
            HoldingsItemsCollectionEntity c1 = fill(HoldingsItemsCollectionEntity.from(em, 870970, "25912233", "i1", Instant.MIN));
            fill(c1.item("a", Instant.MIN))
                    .setStatus(HoldingsItemsStatus.ON_LOAN);
            fill(c1.item("b", Instant.MIN))
                    .setStatus(HoldingsItemsStatus.ON_LOAN);
            fill(c1.item("c", Instant.MIN))
                    .setStatus(HoldingsItemsStatus.ON_SHELF);
            c1.save();
            HoldingsItemsCollectionEntity c2 = fill(HoldingsItemsCollectionEntity.from(em, 870970, "25912233", "i2", Instant.MIN));
            fill(c1.item("d", Instant.MIN))
                    .setStatus(HoldingsItemsStatus.ON_ORDER);
            fill(c1.item("e", Instant.MIN))
                    .setStatus(HoldingsItemsStatus.ON_SHELF);
            c2.save();
        });

        flushAndEvict();

        Map<HoldingsItemsStatus, Long> statusMap = jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            return dao.getStatusFor("25912233", 870970);
        });
        System.out.println("statusMap = " + statusMap);

        assertThat(statusMap.keySet(), hasItems(HoldingsItemsStatus.ON_LOAN,
                                                HoldingsItemsStatus.ON_SHELF,
                                                HoldingsItemsStatus.ON_ORDER));
        assertThat(statusMap.get(HoldingsItemsStatus.ON_LOAN), is(2L));
        assertThat(statusMap.get(HoldingsItemsStatus.ON_SHELF), is(2L));
        assertThat(statusMap.get(HoldingsItemsStatus.ON_ORDER), is(1L));
        assertThat(statusMap.size(), is(3));
    }

    @Test(timeout = 2_000L)
    public void hasLiveHoldingsForAgencyAndBibliographicRecordId() throws Exception {
        System.out.println("hasLiveHoldingsForAgencyAndBibliographicRecordId");
        // Create live holding
        jpa(em -> {
            HoldingsItemsCollectionEntity c1 = fill(HoldingsItemsCollectionEntity.from(em, 870970, "25912233", "i1", Instant.MIN));
            fill(c1.item("a", Instant.MIN))
                    .setStatus(HoldingsItemsStatus.ON_SHELF);
            c1.save();
        });

        boolean hasLiveHoldings = jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            return dao.hasLiveHoldings("25912233", 870970);
        });
        assertTrue(hasLiveHoldings);

        // set to decommissioned
        jpa(em -> {
            HoldingsItemsCollectionEntity c1 = HoldingsItemsCollectionEntity.from(em, 870970, "25912233", "i1", Instant.MIN);
            c1.item("a", Instant.MIN)
                    .setStatus(HoldingsItemsStatus.DECOMMISSIONED);
            c1.save();
        });

        hasLiveHoldings = jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            return dao.hasLiveHoldings("25912233", 870970);
        });
        assertFalse(hasLiveHoldings);

        // Create other issue with live
        jpa(em -> {
            HoldingsItemsCollectionEntity c2 = fill(HoldingsItemsCollectionEntity.from(em, 870970, "25912233", "i2", Instant.MIN));
            fill(c2.item("b", Instant.MIN))
                    .setStatus(HoldingsItemsStatus.ON_SHELF);
            c2.save();
        });

        hasLiveHoldings = jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            return dao.hasLiveHoldings("25912233", 870970);
        });
        assertTrue(hasLiveHoldings);
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

    private void make4(EntityManager em) {
        HoldingsItemsCollectionEntity c1 = HoldingsItemsCollectionEntity.from(em, 870970, "25912233", "i1", Instant.MIN);
        fill(c1);
        HoldingsItemsItemEntity i1 = c1.item("a", Instant.MIN);
        fill(i1);
        c1.save();

        HoldingsItemsCollectionEntity c2 = HoldingsItemsCollectionEntity.from(em, 870970, "25912233", "i2", Instant.MIN);
        fill(c2);
        HoldingsItemsItemEntity i2 = c2.item("b", Instant.MIN);
        fill(i2);
        c2.save();

        HoldingsItemsCollectionEntity c3 = HoldingsItemsCollectionEntity.from(em, 123456, "25912233", "i1", Instant.MIN);
        fill(c3);
        HoldingsItemsItemEntity i3 = c3.item("c", Instant.MIN);
        fill(i3);
        c3.save();

        HoldingsItemsCollectionEntity c4 = HoldingsItemsCollectionEntity.from(em, 870970, "abc", "i1", Instant.MIN);
        fill(c4);
        HoldingsItemsItemEntity i4 = c4.item("d", Instant.MIN);
        fill(i4);
        c4.save();
    }

}
