package dk.dbc.holdingsitems.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.holdingsitems.content.response.AgenciesWithHoldingsResponse;
import dk.dbc.holdingsitems.content.response.CompleteBibliographic;
import dk.dbc.holdingsitems.content.response.ContentServiceBranchResponse;
import dk.dbc.holdingsitems.content.response.ContentServiceItemResponse;
import dk.dbc.holdingsitems.content.response.ContentServiceLaesekompasResponse;
import dk.dbc.holdingsitems.content.response.ContentServicePidResponse;
import dk.dbc.holdingsitems.content.response.LaesekompasHoldingsEntity;
import dk.dbc.holdingsitems.content.response.ResponseHoldingEntity;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.Status;
import dk.dbc.holdingsitems.jpa.SupersedesEntity;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

public class ContentIT extends JpaBase {

    private static final ObjectMapper O = new ObjectMapper();

    @Test(timeout = 30_000L)
    public void testGetItemEntityNoData() throws Exception {
        System.out.println("Test getItemEntity endpoint, no data...");
        ContentServiceItemResponse itemResponse = jpa(em -> {
            ContentResource contentResource = MockContentResource(em);
            Response response = contentResource.getItemEntity(654322, "1234", "track1");
            return (ContentServiceItemResponse) response.getEntity();
        });
        assertNotNull(itemResponse);
        assertEquals(itemResponse.trackingId, "track1");
        assertEquals(itemResponse.holdings.size(), 0);
    }

    @Test(timeout = 30_000L)
    public void testGetItemEntityNoAgencyFound() throws Exception {
        System.out.println("Test getItemEntity endpoint, non-existing agency...");
        ContentServiceItemResponse itemResponse = jpa(em -> {
            ContentResource contentResource = MockContentResource(em);
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, 654321, "12345678", Instant.now(), LocalDate.now());
            bibliographicItemEntity.setTrackingId("somethingToMakeTestHappier");
            IssueEntity issueEntity = bibliographicItemEntity.issue("issue1", Instant.now());
            issueEntity.setTrackingId("somethingToMakeTestHappier");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            itemEntity(issueEntity, "1234", Status.ON_SHELF);
            itemEntity(issueEntity, "2345", Status.ON_LOAN);
            bibliographicItemEntity.save();
            Response response = contentResource.getItemEntity(654322, "1234", "track1");
            return (ContentServiceItemResponse) response.getEntity();
        });
        assertNotNull(itemResponse);
        assertEquals(itemResponse.trackingId, "track1");
        assertEquals(itemResponse.holdings.size(), 0);
    }

    @Test(timeout = 30_000L)
    public void testGetItemEntityNoItemFound() throws Exception {
        System.out.println("Test getItemEntity endpoint, non-existing item id...");
        ContentServiceItemResponse itemResponse = jpa(em -> {
            ContentResource contentResource = MockContentResource(em);
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, 654321, "12345678", Instant.now(), LocalDate.now());
            bibliographicItemEntity.setTrackingId("somethingToMakeTestHappier");
            IssueEntity issueEntity = bibliographicItemEntity.issue("issue1", Instant.now());
            issueEntity.setTrackingId("somethingToMakeTestHappier");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            itemEntity(issueEntity, "1234", Status.ON_SHELF);
            itemEntity(issueEntity, "2345", Status.ON_LOAN);
            bibliographicItemEntity.save();
            Response response = contentResource.getItemEntity(654321, "1244", "track1");
            return (ContentServiceItemResponse) response.getEntity();
        });
        assertNotNull(itemResponse);
        assertEquals(itemResponse.trackingId, "track1");
        assertEquals(itemResponse.holdings.size(), 0);
    }

    @Test(timeout = 30_000L)
    public void testGetItemEntity() throws Exception {
        System.out.println("Test getItemEntity endpoint...");
        ContentServiceItemResponse itemResponse = jpa(em -> {
            ContentResource contentResource = MockContentResource(em);
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, 654321, "12345678", Instant.now(), LocalDate.now());
            bibliographicItemEntity.setTrackingId("somethingToMakeTestHappier");
            IssueEntity issueEntity = bibliographicItemEntity.issue("issue1", Instant.now());
            issueEntity.setTrackingId("somethingToMakeTestHappier");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            itemEntity(issueEntity, "1234", Status.ON_SHELF);
            itemEntity(issueEntity, "2345", Status.ON_LOAN);
            bibliographicItemEntity.save();
            Response response = contentResource.getItemEntity(654321, "1234", "trackItemEntity");
            return (ContentServiceItemResponse) response.getEntity();
        });
        assertNotNull(itemResponse);
        assertEquals(itemResponse.trackingId, "trackItemEntity");
        ResponseHoldingEntity rhe = itemResponse.holdings.get(0);
        assertEquals(rhe.itemId, "1234");
    }

    @Test(timeout = 30_000L)
    public void testGetItemEntitiesNoAgency() throws Exception {
        System.out.println("Test getItemEntity endpoint, no agency argument");
        Response itemResponse = jpa(em -> {
            ContentResource contentResource = MockContentResource(em);
            return contentResource.getItemEntity(-1, "1234", "trackItemEntity");
        });
        assertEquals(itemResponse.getStatusInfo().getStatusCode(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test(timeout = 30_000L)
    public void testGetItemEntitiesNoItem() throws Exception {
        System.out.println("Test getItemEntity endpoint, no item argument");
        Response itemResponse = jpa(em -> {
            ContentResource contentResource = MockContentResource(em);
            return contentResource.getItemEntity(123456, "", "trackItemEntity");
        });
        assertEquals(itemResponse.getStatusInfo().getStatusCode(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test(timeout = 2_000L)
    public void testGetByBranch() throws Exception {
        System.out.println("testGetByBranch");
        ContentServiceBranchResponse branchResponse = jpa(em -> {
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, 654321, "25912233", Instant.now(), LocalDate.now());
            System.out.println("bibliographicItemEntity = " + bibliographicItemEntity);
            bibliographicItemEntity.setTrackingId("somethingToMakeTestHappier");
            IssueEntity issueEntity = bibliographicItemEntity.issue("issue1", Instant.now());
            issueEntity.setTrackingId("somethingToMakeTestHappier");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            itemEntity(issueEntity, "1234", Status.ON_SHELF).setBranchId("123456");
            itemEntity(issueEntity, "2345", Status.ON_LOAN).setBranchId("123456");
            itemEntity(issueEntity, "3456", Status.ON_LOAN).setBranchId("000000");
            bibliographicItemEntity.save();
            ContentResource contentResource = MockContentResource(em);
            Response response = contentResource.getByBranch(654321, "123456", Arrays.asList("870970-basis:25912233"), "trackItemEntity");
            return (ContentServiceBranchResponse) response.getEntity();
        });
        assertThat(branchResponse.completeItems.size(), is(2));
        assertTrue(branchResponse.completeItems.stream().anyMatch(b -> b.itemId.equals("1234")));
        assertTrue(branchResponse.completeItems.stream().anyMatch(b -> b.itemId.equals("2345")));
        assertTrue(branchResponse.completeItems.stream().noneMatch(b -> b.itemId.equals("3456")));
    }

    @Test(timeout = 30_000L)
    public void testGetPidEntitiesOnePid() throws Exception {
        System.out.println("Test getItemByPid endpoint, one pid");
        ContentServicePidResponse pidResponse = jpa(em -> {
            ContentResource contentResource = MockContentResource(em);
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, 654321, "12345678", Instant.now(), LocalDate.now());
            bibliographicItemEntity.setTrackingId("somethingToMakeTestHappier");
            IssueEntity issueEntity = bibliographicItemEntity.issue("issue1", Instant.now());
            issueEntity.setTrackingId("somethingToMakeTestHappier");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            itemEntity(issueEntity, "1234", Status.ON_SHELF);
            itemEntity(issueEntity, "2345", Status.ON_LOAN);
            bibliographicItemEntity.save();
            Response response = contentResource.getItemEntities(654321, Arrays.asList("hest:12345678"), "trackPidOnePid");
            return (ContentServicePidResponse) response.getEntity();
        });
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

    @Test(timeout = 30_000L)
    public void testGetPidEntitiesTwoPids() throws Exception {
        System.out.println("Test getItemByPid endpoint, two pids");
        ContentServicePidResponse pidResponse = jpa(em -> {
            ContentResource contentResource = MockContentResource(em);
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, 654321, "12345678", Instant.now(), LocalDate.now());
            bibliographicItemEntity.setTrackingId("somethingToMakeTestHappier");
            IssueEntity issueEntity = bibliographicItemEntity.issue("issue1", Instant.now());
            issueEntity.setTrackingId("somethingToMakeTestHappier");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            itemEntity(issueEntity, "1234", Status.ON_SHELF);
            itemEntity(issueEntity, "2345", Status.ON_LOAN);
            bibliographicItemEntity.save();

            BibliographicItemEntity bibliographicItemEntity2 = BibliographicItemEntity.from(em, 654321, "87654321", Instant.now(), LocalDate.now());
            bibliographicItemEntity2.setTrackingId("somethingToMakeTestHappier");
            IssueEntity issueEntity2 = bibliographicItemEntity2.issue("issue2", Instant.now());
            issueEntity2.setTrackingId("somethingToMakeTestHappier");
            issueEntity2.setIssueText("#2");
            issueEntity2.setReadyForLoan(1);
            itemEntity(issueEntity2, "4321", Status.ON_SHELF);
            bibliographicItemEntity2.save();

            Response response = contentResource.getItemEntities(654321, Arrays.asList("hest:12345678", "fest:87654321"), "trackPidTwoPids");
            return (ContentServicePidResponse) response.getEntity();
        });
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

    @Test(timeout = 30_000L)
    public void testGetPidEntitiesNonExistingPid() throws Exception {
        System.out.println("Test getItemByPid endpoint, non-existing pid");
        ContentServicePidResponse pidResponse = jpa(em -> {
            ContentResource contentResource = MockContentResource(em);
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, 654321, "12345678", Instant.now(), LocalDate.now());
            bibliographicItemEntity.setTrackingId("somethingToMakeTestHappier");
            IssueEntity issueEntity = bibliographicItemEntity.issue("issue1", Instant.now());
            issueEntity.setTrackingId("somethingToMakeTestHappier");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            itemEntity(issueEntity, "1234", Status.ON_SHELF);
            itemEntity(issueEntity, "2345", Status.ON_LOAN);
            bibliographicItemEntity.save();
            Response response = contentResource.getItemEntities(654321, Arrays.asList("hest:123456789"), "trackPidNonExistingPid");
            return (ContentServicePidResponse) response.getEntity();
        });
        assertNotNull(pidResponse);
        assertEquals(pidResponse.trackingId, "trackPidNonExistingPid");
        Map<String, List<ResponseHoldingEntity>> holdingsMap = pidResponse.holdings;
        assertTrue(holdingsMap.containsKey("hest:123456789"));
        List<ResponseHoldingEntity> holdings = holdingsMap.get("hest:123456789");
        assertNotNull(holdings);
        assertEquals(holdings.size(), 0);
    }

    @Test(timeout = 30_000L)
    public void testGetPidEntitiesRepeatedPid() throws Exception {
        System.out.println("Test getItemByPid endpoint, repeated pid");
        Response response = jpa(em -> {
            ContentResource contentResource = MockContentResource(em);
            return contentResource.getItemEntities(654321, Arrays.asList("hest:12345678", "fest:12345678"), "trackPidRepeatedPid");
        });
        assertEquals(response.getStatusInfo().getStatusCode(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test(timeout = 30_000L)
    public void testGetPidEntitiesNoAgency() throws Exception {
        System.out.println("Test getItemByPid endpoint, no agency");
        Response response = jpa(em -> {
            ContentResource contentResource = MockContentResource(em);
            return contentResource.getItemEntities(-1, Arrays.asList("hest:12345678"), "trackPidNoAgency");
        });
        assertEquals(response.getStatusInfo().getStatusCode(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    private ContentResource MockContentResource(EntityManager em) {
        ContentResource mock = mock(ContentResource.class);
        mock.em = em;
        doCallRealMethod().when(mock).getItemEntity(anyInt(), anyString(), anyString());
        doCallRealMethod().when(mock).getItemEntities(anyInt(), anyList(), anyString());
        doCallRealMethod().when(mock).getByBranch(anyInt(), anyString(), anyList(), anyString());
        doCallRealMethod().when(mock).getLaesekompasdataForBibliographicRecordIdsPost(anyString(), anyString());
        doCallRealMethod().when(mock).getComplete(anyInt(), anyString(), anyString());
        return mock;
    }

    @Test(timeout = 30_000L)
    public void testGetLaesekompasDataOnePidPost() throws Exception {
        System.out.println("Test POST getItemByPid endpoint, one pid");
        ContentServiceLaesekompasResponse pidResponse = jpa(em -> {
            ContentResource contentResource = MockContentResource(em);
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, 654321, "23456781", Instant.now(), LocalDate.now());
            bibliographicItemEntity.setTrackingId("somethingToMakeTestHappier");
            IssueEntity issueEntity = bibliographicItemEntity.issue("issue1", Instant.now());
            issueEntity.setTrackingId("somethingToMakePostTestHappier");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            itemEntity(issueEntity, "2341", Status.ON_SHELF);
            itemEntity(issueEntity, "3452", Status.ON_LOAN);
            bibliographicItemEntity.save();
            Response response = contentResource.getLaesekompasdataForBibliographicRecordIdsPost(O.writeValueAsString(Arrays.asList("23456781")), "trackPidOnePidPost");
            return (ContentServiceLaesekompasResponse) response.getEntity();
        });
        assertNotNull(pidResponse);
        assertEquals(pidResponse.trackingId, "trackPidOnePidPost");
        Map<String, Iterable<LaesekompasHoldingsEntity>> holdingsMap = pidResponse.holdings;
        assertTrue(holdingsMap.containsKey("23456781"));
        List<LaesekompasHoldingsEntity> holdings = StreamSupport.stream(holdingsMap.get("23456781").spliterator(), false).collect(Collectors.toList());
        assertNotNull(holdings);
        assertEquals(holdings.size(), 2);
        List<LaesekompasHoldingsEntity> heOnShelfList = holdings.stream().filter(h -> h.status.equals(Status.ON_SHELF)).collect(Collectors.toList());
        assertEquals(heOnShelfList.size(), 1);
        LaesekompasHoldingsEntity heOnShelf = heOnShelfList.get(0);
        assertNotNull(heOnShelf);
    }

    @Test(timeout = 30_000L)
    public void testGetPidEntitiesTwoPidsPost() throws Exception {
        System.out.println("Test getItemByPid endpoint, two pids");
        ContentServiceLaesekompasResponse pidResponse = jpa(em -> {
            ContentResource contentResource = MockContentResource(em);
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, 654321, "12345678", Instant.now(), LocalDate.now());
            bibliographicItemEntity.setTrackingId("somethingToMakePostTestHappier");
            IssueEntity issueEntity = bibliographicItemEntity.issue("issue1", Instant.now());
            issueEntity.setTrackingId("somethingToMakeTestHappier");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            itemEntity(issueEntity, "1234", Status.ON_SHELF);
            itemEntity(issueEntity, "2345", Status.ON_LOAN);
            bibliographicItemEntity.save();

            BibliographicItemEntity bibliographicItemEntity2 = BibliographicItemEntity.from(em, 654321, "87654321", Instant.now(), LocalDate.now());
            bibliographicItemEntity2.setTrackingId("somethingToMakeTestHappier");
            IssueEntity issueEntity2 = bibliographicItemEntity2.issue("issue2", Instant.now());
            issueEntity2.setTrackingId("somethingToMakeTestHappier");
            issueEntity2.setIssueText("#2");
            issueEntity2.setReadyForLoan(1);
            itemEntity(issueEntity2, "4321", Status.ON_SHELF);
            bibliographicItemEntity2.save();

            Response response = contentResource.getLaesekompasdataForBibliographicRecordIdsPost(O.writeValueAsString(Arrays.asList("12345678", "87654321")), "trackPidTwoPidsPost");
            return (ContentServiceLaesekompasResponse) response.getEntity();
        });
        assertNotNull(pidResponse);
        assertEquals(pidResponse.trackingId, "trackPidTwoPidsPost");
        Map<String, Iterable<LaesekompasHoldingsEntity>> holdingsMap = pidResponse.holdings;
        assertTrue(holdingsMap.containsKey("12345678"));
        List<LaesekompasHoldingsEntity> holdingsHest = StreamSupport.stream(holdingsMap.get("12345678").spliterator(), false).collect(Collectors.toList());
        assertNotNull(holdingsHest);
        assertEquals(holdingsHest.size(), 2);
        List<LaesekompasHoldingsEntity> heOnShelfList = holdingsHest.stream().filter(h -> h.status.equals(Status.ON_SHELF)).collect(Collectors.toList());
        assertEquals(heOnShelfList.size(), 1);
        LaesekompasHoldingsEntity heOnShelf = heOnShelfList.get(0);
        assertNotNull(heOnShelf);

        assertTrue(holdingsMap.containsKey("87654321"));
        List<LaesekompasHoldingsEntity> holdingsFest = StreamSupport.stream(holdingsMap.get("87654321").spliterator(), false).collect(Collectors.toList());
        assertNotNull(holdingsFest);
        assertEquals(holdingsFest.size(), 1);
        LaesekompasHoldingsEntity heFest = holdingsFest.get(0);
        assertNotNull(heFest);
    }

    @Test(timeout = 2_000L)
    public void testGetCompleteNotFound() throws Exception {
        System.out.println("testGetCompleteNotFound");
        Response response = jpa(em -> {
            ContentResource contentResource = MockContentResource(em);
            return contentResource.getComplete(-1, "12345678", "trackPidNoAgency");
        });
        assertEquals(response.getStatusInfo().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test(timeout = 2_000L)
    public void testComplete() throws Exception {
        System.out.println("testComplete");
        jpa(em -> {
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, 654321, "12345678", Instant.now(), LocalDate.now());
            bibliographicItemEntity.setTrackingId("somethingToMakePostTestHappier");
            IssueEntity issueEntity = bibliographicItemEntity.issue("issue1", Instant.now());
            issueEntity.setTrackingId("somethingToMakeTestHappier");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            itemEntity(issueEntity, "1234", Status.ON_SHELF);
            itemEntity(issueEntity, "2345", Status.ON_LOAN);
            bibliographicItemEntity.save();
        });
        Response response = jpa(em -> {
            ContentResource contentResource = MockContentResource(em);
            return contentResource.getComplete(654321, "12345678", "trackPidNoAgency");
        });
        assertEquals(response.getStatusInfo().getStatusCode(), Response.Status.OK.getStatusCode());
        CompleteBibliographic entity = (CompleteBibliographic) response.getEntity();
        assertEquals(entity.issues.size(), 1);
        assertEquals(entity.issues.get(0).issueId, "issue1");
        assertEquals(entity.issues.get(0).items.size(), 2);
        assertEquals(entity.issues.get(0).items.get(0).itemId, "1234");
        assertEquals(entity.issues.get(0).items.get(1).itemId, "2345");
    }

    @Test(timeout = 2_000L)
    public void testAgenciesWithHoldings() throws Exception {
        System.out.println("testAgenciesWithHoldings");

        int agencyId1 = 100000;
        int agencyId2 = 200000;
        String bibId = "10000000";

        jpa(em -> {
            System.out.println(" `- load record 1");
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, agencyId1, bibId, Instant.now(), LocalDate.now());
            bibliographicItemEntity.setTrackingId("track");
            bibliographicItemEntity.setFirstAccessionDate(LocalDate.of(2001, 2, 24));
            bibliographicItemEntity.setNote("NOTE TEXT");
            IssueEntity issueEntity = bibliographicItemEntity.issue("issue1", Instant.now());
            issueEntity.setTrackingId("track");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            itemEntity(issueEntity, "1", Status.ON_SHELF);
            bibliographicItemEntity.save();
        });
        jpa(em -> {
            System.out.println(" `- load record 2");
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, agencyId2, bibId, Instant.now(), LocalDate.now());
            bibliographicItemEntity.setTrackingId("track");
            bibliographicItemEntity.setFirstAccessionDate(LocalDate.of(2012, 11, 7));
            bibliographicItemEntity.setNote("NOTE TEXT");
            IssueEntity issueEntity = bibliographicItemEntity.issue("none", Instant.now());
            issueEntity.setTrackingId("track");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            itemEntity(issueEntity, "2", Status.ON_SHELF);
            bibliographicItemEntity.save();
        });

        jpa(em -> {
            System.out.println(" `- test record 1 - 404");
            ContentResource bean = new ContentResource();
            bean.em = em;
            Response resp = bean.agenciesWithHoldings(bibId, "x");
            assertThat(resp.getStatus(), is(200));
            AgenciesWithHoldingsResponse entity = (AgenciesWithHoldingsResponse) resp.getEntity();
            System.out.println(O.writeValueAsString(entity));
            assertThat(entity, field("agencies", containsInAnyOrder(agencyId1, agencyId2)));
        });
    }

    @Test(timeout = 2_000L)
    public void testCompleteSuperseded() throws Exception {
        System.out.println("testCompleteSuperseded");
        int agencyId = 100000;
        String bibId1 = "10000000";
        String bibId2 = "10000001";

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
            ContentResource bean = new ContentResource();
            bean.em = em;
            Response resp = bean.getComplete(agencyId, bibId1, "x");
            assertThat(resp.getStatus(), is(200));
            CompleteBibliographic entity = (CompleteBibliographic) resp.getEntity();
            System.out.println(O.writeValueAsString(entity));
            assertThat(entity, notNullValue());
            assertThat(entity.issues, containsInAnyOrder(allOf(field("issueId", is("issue1"))),
                                                         allOf(field("issueId", is("issue2")))));
        });

        jpa(em -> {
            System.out.println(" `- record 1 superseded by record 2");
            SupersedesEntity supersedesEntity = new SupersedesEntity(bibId1, bibId2);
            em.persist(supersedesEntity);
        });

        jpa(em -> {
            System.out.println(" `- test record 1 - 404");
            ContentResource bean = new ContentResource();
            bean.em = em;
            Response resp = bean.getComplete(agencyId, bibId1, "x");
            assertThat(resp.getStatus(), is(404));
        });

        jpa(em -> {
            System.out.println(" `- test record 2 - record 1 content");
            ContentResource bean = new ContentResource();
            bean.em = em;
            Response resp = bean.getComplete(agencyId, bibId2, "x");
            assertThat(resp.getStatus(), is(200));
            CompleteBibliographic entity = (CompleteBibliographic) resp.getEntity();
            System.out.println(O.writeValueAsString(entity));
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
            itemEntity(issueEntity1, "3", Status.ON_SHELF);
            itemEntity(issueEntity1, "4", Status.ON_LOAN);
            bibliographicItemEntity.save();
        });

        jpa(em -> {
            System.out.println(" `- test record 2 - record 2 and 1 content");
            ContentResource bean = new ContentResource();
            bean.em = em;
            Response resp = bean.getComplete(agencyId, bibId2, "x");
            assertThat(resp.getStatus(), is(200));
            CompleteBibliographic entity = (CompleteBibliographic) resp.getEntity();
            System.out.println(O.writeValueAsString(entity));
            assertThat("Oldest of the 2", entity.firstAccessionDate, is("2001-02-24"));
            assertThat(entity.note, is("NOTE TEXT"));
            assertThat(entity.issues, containsInAnyOrder(
                       allOf(field("issueId", is("issue1")),
                             field("issueText", is("new#1")),
                             field("items", containsInAnyOrder(
                                   allOf(field("itemId", is("1")),
                                         field("bibliographicRecordId", is(bibId1))),
                                   allOf(field("itemId", is("2")),
                                         field("bibliographicRecordId", is(bibId1))),
                                   allOf(field("itemId", is("3")),
                                         field("bibliographicRecordId", is(bibId2))),
                                   allOf(field("itemId", is("4")),
                                         field("bibliographicRecordId", is(bibId2)))
                           ))),
                       allOf(field("issueId", is("issue2")),
                             field("issueText", is("old#2")),
                             field("items", containsInAnyOrder(
                                   allOf(field("itemId", is("5")),
                                         field("bibliographicRecordId", is(bibId1))),
                                   allOf(field("itemId", is("6")),
                                         field("bibliographicRecordId", is(bibId1)))
                           )))
               ));
        });

        jpa(em -> {
            System.out.println(" `- empty out record 1");
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, agencyId, bibId1, Instant.now(), LocalDate.now());
            Arrays.asList(bibliographicItemEntity.stream().toArray(IssueEntity[]::new))
                    .forEach(bibliographicItemEntity::removeIssue);
            bibliographicItemEntity.save();
        });

        jpa(em -> {
            System.out.println(" `- test record 2 - record 2 and 1(empty) content");
            ContentResource bean = new ContentResource();
            bean.em = em;
            Response resp = bean.getComplete(agencyId, bibId2, "x");
            assertThat(resp.getStatus(), is(200));
            CompleteBibliographic entity = (CompleteBibliographic) resp.getEntity();
            System.out.println(O.writeValueAsString(entity));
            assertThat("Oldest of the 2", entity.firstAccessionDate, is("2001-02-24"));
            assertThat(entity.note, is(""));
            assertThat(entity.issues, contains(
                       allOf(field("issueId", is("issue1")),
                             field("items", containsInAnyOrder(
                                   allOf(field("itemId", is("3")),
                                         field("bibliographicRecordId", is(bibId2))),
                                   allOf(field("itemId", is("4")),
                                         field("bibliographicRecordId", is(bibId2)))
                           )))));
        });
    }

    private ItemEntity itemEntity(IssueEntity issueEntity, String itemId, Status status) {
        ItemEntity itemEntity = issueEntity.item(itemId, Instant.now());
        itemEntity.setAccessionDate(localNow());
        itemEntity.setStatus(status);
        itemEntity.setBranch("branch");
        itemEntity.setBranchId("9876");
        itemEntity.setDepartment("department");
        itemEntity.setLocation("location");
        itemEntity.setSubLocation("subLocation");
        itemEntity.setCirculationRule("");
        return itemEntity;
    }

    private LocalDate localNow() {
        return LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toLocalDate();
    }

    private static <T, R> FieldMatcher field(String field, Matcher<R> matcher) {
        return new FieldMatcher(field, matcher);
    }

    private static class FieldMatcher<T, R> extends BaseMatcher<T> {

        private final String field;
        private final Matcher<R> matcher;
        private String error;

        private FieldMatcher(String field, Matcher<R> matcher) {
            this.field = field;
            this.matcher = matcher;
            this.error = null;
        }

        @Override
        public boolean matches(Object item) {
            if (item == null) {
                error = "object needs to be defined";
            } else {
                try {
                    Field f = item.getClass().getField(field);
                    return matcher.matches(f.get(item));
                } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
                    error = "cannot access field: '" + field + "'";
                }
            }
            return false;
        }

        @Override
        public void describeMismatch(Object item, Description mismatchDescription) {
            if (error == null) {
                mismatchDescription.appendText("." + field + " ");
                matcher.describeMismatch(item, mismatchDescription);
            } else {
                mismatchDescription.appendText(error);
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("." + field + " ");
            matcher.describeTo(description);
        }
    }
}
