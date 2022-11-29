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

import dk.dbc.holdingsitems.jpa.BibliographicItemDetached;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.ItemKey;
import dk.dbc.holdingsitems.jpa.Status;
import dk.dbc.holdingsitems.jpa.SupersedesEntity;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
            try (EnqueueService enqueueService = dao.enqueueService()) {
                enqueueService.enqueue("supa", 888888, "12345678", "{}");
                enqueueService.enqueue("supa", 888888, "87654321", "{}");
            }
        });
        try (Connection connection = PG.createConnection() ;
             Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("SELECT consumer, bibliographicRecordId, agencyId, trackingId FROM queue")) {
            HashSet<String> results = new HashSet<>();
            while (resultSet.next()) {
                int i = 0;
                String consumer = resultSet.getString(++i);
                String biblId = resultSet.getString(++i);
                int agencyId = resultSet.getInt(++i);
                String tracking = resultSet.getString(++i);
                results.add(consumer + "|" + biblId + "|" + agencyId + "|" + tracking);
            }
            System.out.println("results = " + results);
            assertEquals(2, results.size());
            assertTrue(results.contains("consa|12345678|888888|FOO"));
            assertTrue(results.contains("consa|87654321|888888|FOO"));
        }
    }

    @Test(timeout = 2_000L)
    public void allLiveBibliographicIdsForAgency() throws Exception {
        System.out.println("allLiveBibliographicIdsForAgency");

        jpa(em -> {
            make4(em);
        });

        jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            Set<String> bibIds = dao.getBibliographicIds(870970);
            System.out.println("bibIds = " + bibIds);
            assertThat(bibIds, hasItems("25912233", "abc"));
        });

        // Decommission abc:i1:d
        jpa(em -> {
            BibliographicItemEntity b = BibliographicItemEntity.from(em, 870970, "abc", Instant.now(), LocalDate.MAX);
            IssueEntity c = b.issue("i1", Instant.MIN);
            c.item("d", Instant.MIN)
                    .remove();
            em.merge(b);
        });

        jpa(em -> {
            List<String> bibIdsDecom = em.createQuery("SELECT h.bibliographicRecordId" +
                                                      " FROM ItemEntity h" +
                                                      " WHERE h.agencyId = :agencyId" +
                                                      " GROUP BY h.agencyId, h.bibliographicRecordId",
                                                      String.class)
                    .setParameter("agencyId", 870970)
                    .getResultList();
            System.out.println("bibIds = " + bibIdsDecom);
            assertThat(bibIdsDecom, hasItems("25912233"));
        });
    }

    @Test(timeout = 2_000L)
    public void allHoldingsItemIdsForAgencyWithSupercedes() throws Exception {
        System.out.println("allHoldingsItemIdsForAgencyWithSupercedes");
        jpa(em -> {
            make4(em);
            makeSupercede(em);
        });

        jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            List<String> ids = dao.getHoldingItems(870970);
            System.out.println("issues = " + ids);
            assertThat(ids, hasItems("25912233", "def"));
        });
    }

    @Test(timeout = 2_000L)
    public void allIssuesForAgencyAndBibliographicRecordId() throws Exception {
        System.out.println("allIssuesForAgencyAndBibliographicRecordId");
        jpa(em -> {
            make4(em);
        });

        jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            Set<String> issues = dao.getIssueIds("25912233", 870970);
            System.out.println("issues = " + issues);
            assertThat(issues, hasItems("i1", "i2"));
        });
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

        jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            Map<Status, Long> statusMap = dao.getStatusFor("25912233", 870970);
            System.out.println("statusMap = " + statusMap);

            assertThat(statusMap.keySet(), hasItems(Status.ON_LOAN,
                                                    Status.ON_SHELF,
                                                    Status.ON_ORDER));
            assertThat(statusMap.get(Status.ON_LOAN), is(2L));
            assertThat(statusMap.get(Status.ON_SHELF), is(2L));
            assertThat(statusMap.get(Status.ON_ORDER), is(1L));
            assertThat(statusMap.size(), is(3));
        });
    }

    @Test(timeout = 12_000L)
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

        jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            boolean hasLiveHoldings = dao.hasLiveHoldings("25912233", 870970);
            assertTrue(hasLiveHoldings);
        });

        // set to decommissioned
        jpa(em -> {
            BibliographicItemEntity b1 = BibliographicItemEntity.from(em, 870970, "25912233", Instant.MIN, LocalDate.now());
            IssueEntity c1 = b1.issue("i1", Instant.MIN);
            c1.item("a", Instant.MIN)
                    .remove();
            b1.save();
        });

        jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            boolean hasLiveHoldings = dao.hasLiveHoldings("25912233", 870970);
            assertFalse(hasLiveHoldings);
        });

        // Create other issue with live
        jpa(em -> {
            BibliographicItemEntity b1 = BibliographicItemEntity.from(em, 870970, "25912233", Instant.MIN, LocalDate.now());
            IssueEntity c2 = fill(b1.issue("i2", Instant.MIN));
            fill(c2.item("b", Instant.MIN))
                    .setStatus(Status.ON_SHELF);
            c2.save();
        });

        jpa(em -> {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
            boolean hasLiveHoldings = dao.hasLiveHoldings("25912233", 870970);
            assertTrue(hasLiveHoldings);
        });
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

        jpa(em -> {
            ItemEntity item = em.find(ItemEntity.class, new ItemKey(870970, "25912233", "i1", "it1"));
            System.out.println("item = " + item);
            assertThat(item, notNullValue());
        });
    }

    @Test(timeout = 2_000L)
    public void testMergeSuperseded() throws Exception {
        System.out.println("testMergeSuperseded");

        int agencyId = 100000;
        String bibId1 = "rec1";
        String bibId2 = "rec2";

        jpa(em -> {
            System.out.println(" `- load record 1");
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, agencyId, bibId1, Instant.now(), LocalDate.now());
            bibliographicItemEntity.setTrackingId("track");
            bibliographicItemEntity.setFirstAccessionDate(LocalDate.of(2001, 2, 24));
            bibliographicItemEntity.setNote("NOTE TEXT");
            IssueEntity issueEntity1 = bibliographicItemEntity.issue("issue1", Instant.now());
            issueEntity1.setTrackingId("track");
            issueEntity1.setIssueText("old#1");
            issueEntity1.setReadyForLoan(1);
            itemEntity(issueEntity1, "1", Status.ON_SHELF);
            itemEntity(issueEntity1, "2", Status.ON_LOAN);
            IssueEntity issueEntity2 = bibliographicItemEntity.issue("issue2", Instant.now());
            issueEntity2.setTrackingId("track");
            issueEntity2.setIssueText("old#2");
            issueEntity2.setReadyForLoan(1);
            itemEntity(issueEntity2, "5", Status.ON_SHELF);
            itemEntity(issueEntity2, "6", Status.ON_LOAN);
            bibliographicItemEntity.save();
        });

        jpa(em -> {
            System.out.println(" `- test record 1");
            BibliographicItemDetached entity = BibliographicItemEntity.detachedWithSuperseded(em, agencyId, bibId1);
            assertThat(entity, notNullValue());
        });

        jpa(em -> {
            System.out.println(" `- record 1 superseded by record 2");
            SupersedesEntity supersedesEntity = new SupersedesEntity(bibId1, bibId2);
            em.persist(supersedesEntity);
        });

        jpa(em -> {
            System.out.println(" `- test record 1 superseded");
            BibliographicItemDetached entity = BibliographicItemEntity.detachedWithSuperseded(em, agencyId, bibId1);
            assertThat(entity, nullValue());
        });

        jpa(em -> {
            System.out.println(" `- test record 2 with record 1 content");
            BibliographicItemDetached entity = BibliographicItemEntity.detachedWithSuperseded(em, agencyId, bibId2);
            assertThat(entity, notNullValue());
            System.out.println("entity = " + entity);
            entity.getIssues().forEach(is -> {
                System.out.println("is = " + is);
                is.getItems().forEach(it -> {
                    System.out.println("it = " + it);
                });
            });
            assertThat(entity.getNote(), is("NOTE TEXT"));
            assertThat(entity.getFirstAccessionDate(), is(LocalDate.of(2001, 2, 24)));
            assertThat(entity.getIssues(), containsInAnyOrder(
                       allOf(method("getIssueId", is("issue1")),
                             method("getItems", containsInAnyOrder(
                                    allOf(method("getItemId", is("1"))),
                                    allOf(method("getItemId", is("2")))))),
                       allOf(method("getIssueId", is("issue2")),
                             method("getItems", containsInAnyOrder(
                                    allOf(method("getItemId", is("5"))),
                                    allOf(method("getItemId", is("6"))))))
               ));
        });

        jpa(em -> {
            System.out.println(" `- load record 2");
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, agencyId, bibId2, Instant.now(), LocalDate.now());
            bibliographicItemEntity.setTrackingId("track");
            bibliographicItemEntity.setFirstAccessionDate(LocalDate.of(2012, 4, 12));
            IssueEntity issueEntity1 = bibliographicItemEntity.issue("issue1", Instant.now());
            issueEntity1.setTrackingId("track");
            issueEntity1.setIssueText("new#1");
            issueEntity1.setReadyForLoan(1);
            itemEntity(issueEntity1, "2", Status.ON_SHELF).setBranch("THE NEW ONE");
            itemEntity(issueEntity1, "3", Status.ON_SHELF);
            itemEntity(issueEntity1, "4", Status.ON_LOAN);
            bibliographicItemEntity.save();
        });

        jpa(em -> {
            System.out.println(" `- test record 2 with record 2+1 content");
            BibliographicItemDetached entity = BibliographicItemEntity.detachedWithSuperseded(em, agencyId, bibId2);
            assertThat(entity, notNullValue());
            System.out.println("entity = " + entity);
            assertThat(entity.getNote(), is("NOTE TEXT"));
            assertThat(entity.getFirstAccessionDate(), is(LocalDate.of(2001, 2, 24)));
            assertThat(entity.getIssues(), containsInAnyOrder(
                       allOf(method("getIssueId", is("issue1")),
                             method("getItems", containsInAnyOrder(
                                    allOf(method("getItemId", is("1"))),
                                    allOf(method("getItemId", is("2")),
                                          method("getBranch", is("THE NEW ONE"))),
                                    allOf(method("getItemId", is("3"))),
                                    allOf(method("getItemId", is("4")))
                            ))),
                       allOf(method("getIssueId", is("issue2")),
                             method("getItems", containsInAnyOrder(
                                    allOf(method("getItemId", is("5"))),
                                    allOf(method("getItemId", is("6")))
                            )))));
        });

        jpa(em -> {
            System.out.println(" `- clear record 1");
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, agencyId, bibId1, Instant.now(), LocalDate.now());
            bibliographicItemEntity.removeIssue(bibliographicItemEntity.issue("issue1", Instant.now()));
            bibliographicItemEntity.removeIssue(bibliographicItemEntity.issue("issue2", Instant.now()));
            bibliographicItemEntity.save();
        });

        jpa(em -> {
            System.out.println(" `- test record 2 with record 2 content");
            BibliographicItemDetached entity = BibliographicItemEntity.detachedWithSuperseded(em, agencyId, bibId2);
            assertThat(entity, notNullValue());
            System.out.println("entity = " + entity);
            assertThat(entity.getNote(), is("")); // Note note from superseded
            assertThat(entity.getFirstAccessionDate(), is(LocalDate.of(2001, 2, 24)));
            assertThat(entity.getIssues(), containsInAnyOrder(
                       allOf(method("getIssueId", is("issue1")),
                             method("getItems", containsInAnyOrder(
                                    allOf(method("getItemId", is("2"))),
                                    allOf(method("getItemId", is("3"))),
                                    allOf(method("getItemId", is("4")))
                            )))));
        });
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

    private void makeSupercede(EntityManager em) {
        em.persist(new SupersedesEntity("abc", "def"));
    }

    private ItemEntity itemEntity(IssueEntity issueEntity, String itemId, Status status) {
        ItemEntity itemEntity = issueEntity.item(itemId, Instant.now());
        itemEntity.setAccessionDate(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toLocalDate());
        itemEntity.setStatus(status);
        itemEntity.setBranch("branch");
        itemEntity.setBranchId("9876");
        itemEntity.setDepartment("department");
        itemEntity.setLocation("location");
        itemEntity.setSubLocation("subLocation");
        itemEntity.setCirculationRule("");
        return itemEntity;
    }

    private static <T, R> MethodMatcher method(String method, Matcher<R> matcher) {
        return new MethodMatcher(method, matcher);
    }

    private static class MethodMatcher<T, R> extends BaseMatcher<T> {

        private final String method;
        private final Matcher<R> matcher;
        private String error;

        private MethodMatcher(String method, Matcher<R> matcher) {
            this.method = method;
            this.matcher = matcher;
            this.error = null;
        }

        @Override
        public boolean matches(Object item) {
            if (item == null) {
                error = "object needs to be defined";
            } else {
                try {
                    Method m = item.getClass().getMethod(method);
                    return matcher.matches(m.invoke(item));
                } catch (NoSuchMethodException ex) {
                    error = "object hasn't got method: " + method + "()";
                } catch (SecurityException ex) {
                    error = "cannot access method: " + method + "()";
                } catch (IllegalAccessException ex) {
                    error = "cannot access method: " + method + "()";
                } catch (IllegalArgumentException ex) {
                    error = "cannot access method: " + method + "()";
                } catch (InvocationTargetException ex) {
                    error = "cannot access method: " + method + "()";
                }
            }
            return false;
        }

        @Override
        public void describeMismatch(Object item, Description mismatchDescription) {
            if (error == null) {
                mismatchDescription.appendText(method + "() ");
                matcher.describeMismatch(item, mismatchDescription);
            } else {
                mismatchDescription.appendText(error);
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(method + "() ");
            matcher.describeTo(description);
        }
    }
}
