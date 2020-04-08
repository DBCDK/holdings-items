import dk.dbc.holdingsitems.content.ContentResource;
import dk.dbc.holdingsitems.content.response.ContentServiceItemResponse;
import dk.dbc.holdingsitems.content.response.ContentServicePidResponse;
import dk.dbc.holdingsitems.content.response.ResponseHoldingEntity;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.Status;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

public class ContentIT extends JpaBase {

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
            IssueEntity issueEntity = IssueEntity.from(em, bibliographicItemEntity, "issue1");
            issueEntity.setTrackingId("somethingToMakeTestHappier");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            bibliographicItemEntity.save();
            itemEntity(issueEntity, "1234", Status.ON_SHELF);
            itemEntity(issueEntity, "2345", Status.ON_LOAN);
            issueEntity.save();
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
            IssueEntity issueEntity = IssueEntity.from(em, bibliographicItemEntity, "issue1");
            issueEntity.setTrackingId("somethingToMakeTestHappier");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            bibliographicItemEntity.save();
            itemEntity(issueEntity, "1234", Status.ON_SHELF);
            itemEntity(issueEntity, "2345", Status.ON_LOAN);
            issueEntity.save();
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
            IssueEntity issueEntity = IssueEntity.from(em, bibliographicItemEntity, "issue1");
            issueEntity.setTrackingId("somethingToMakeTestHappier");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            bibliographicItemEntity.save();
            itemEntity(issueEntity, "1234", Status.ON_SHELF);
            itemEntity(issueEntity, "2345", Status.ON_LOAN);
            issueEntity.save();
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

    @Test(timeout = 30_000L)
    public void testGetPidEntitiesOnePid() throws Exception {
        System.out.println("Test getItemByPid endpoint, one pid");
        ContentServicePidResponse pidResponse = jpa(em -> {
            ContentResource contentResource = MockContentResource(em);
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, 654321, "12345678", Instant.now(), LocalDate.now());
            bibliographicItemEntity.setTrackingId("somethingToMakeTestHappier");
            IssueEntity issueEntity = IssueEntity.from(em, bibliographicItemEntity, "issue1");
            issueEntity.setTrackingId("somethingToMakeTestHappier");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            bibliographicItemEntity.save();
            itemEntity(issueEntity, "1234", Status.ON_SHELF);
            itemEntity(issueEntity, "2345", Status.ON_LOAN);
            issueEntity.save();
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
            IssueEntity issueEntity = IssueEntity.from(em, bibliographicItemEntity, "issue1");
            issueEntity.setTrackingId("somethingToMakeTestHappier");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            bibliographicItemEntity.save();
            itemEntity(issueEntity, "1234", Status.ON_SHELF);
            itemEntity(issueEntity, "2345", Status.ON_LOAN);
            issueEntity.save();

            BibliographicItemEntity bibliographicItemEntity2 = BibliographicItemEntity.from(em, 654321, "87654321", Instant.now(), LocalDate.now());
            bibliographicItemEntity2.setTrackingId("somethingToMakeTestHappier");
            IssueEntity issueEntity2 = IssueEntity.from(em, bibliographicItemEntity2, "issue2");
            issueEntity2.setTrackingId("somethingToMakeTestHappier");
            issueEntity2.setIssueText("#2");
            issueEntity2.setReadyForLoan(1);
            bibliographicItemEntity2.save();
            itemEntity(issueEntity2, "4321", Status.ON_SHELF);
            issueEntity2.save();

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
            IssueEntity issueEntity = IssueEntity.from(em, bibliographicItemEntity, "issue1");
            issueEntity.setTrackingId("somethingToMakeTestHappier");
            issueEntity.setIssueText("#1");
            issueEntity.setReadyForLoan(1);
            bibliographicItemEntity.save();
            itemEntity(issueEntity, "1234", Status.ON_SHELF);
            itemEntity(issueEntity, "2345", Status.ON_LOAN);
            issueEntity.save();
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
        return mock;
    }

    private void itemEntity(IssueEntity issueEntity, String itemId, Status status) {
        ItemEntity itemEntity = issueEntity.item(itemId, Instant.now());
        itemEntity.setAccessionDate(localNow());
        itemEntity.setStatus(status);
        itemEntity.setBranch("branch");
        itemEntity.setDepartment("department");
        itemEntity.setLocation("location");
        itemEntity.setSubLocation("subLocation");
        itemEntity.setCirculationRule("");
    }

    private LocalDate localNow() {
        return LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toLocalDate();
    }

}
