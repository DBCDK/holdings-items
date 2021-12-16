package dk.dbc.holdingsitems.content;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.mdc.GenerateTrackingId;
import dk.dbc.commons.mdc.LogAs;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.content.response.CompleteBibliographic;
import dk.dbc.holdingsitems.content.response.CompleteItemFull;
import dk.dbc.holdingsitems.content.response.ContentServiceBranchResponse;
import dk.dbc.holdingsitems.content.response.ContentServiceLaesekompasResponse;
import dk.dbc.holdingsitems.content.response.ContentServiceItemResponse;
import dk.dbc.holdingsitems.content.response.ContentServicePidResponse;
import dk.dbc.holdingsitems.content.response.IndexHtml;
import dk.dbc.holdingsitems.content.response.LaesekompasHoldingsEntity;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.Status;
import dk.dbc.log.LogWith;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.PathParam;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Stateless
@Path("/")
@Produces("application/json")
public class ContentResource {

    @Inject
    public EntityManager em;

    @Inject
    public IndexHtml indexHtml;
    private static final ObjectMapper O = new ObjectMapper()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static final Logger log = LoggerFactory.getLogger(ContentResource.class);

    @GET
    @Path("holdings-by-item-id")
    public Response getItemEntity(
            @QueryParam("agency") Integer agencyId,
            @QueryParam("itemId") String itemId,
            @QueryParam("trackingId") @LogAs("trackingId") @GenerateTrackingId String trackingId) {
        { // argument validation
            if (agencyId == null || agencyId < 0) {
                log.error("holdings-by-item-id called with no agency");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (!( itemId.trim().length() > 0 )) {
                log.error("holdings-by-item-id called with no item");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }
        log.debug("holdings-by-item-id called with agency: {}, itemId: {}, trackingId: {}", agencyId, itemId, trackingId);
        HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, trackingId);
        Set<ItemEntity> itemEntities = dao.getItemsFromAgencyIdAndItemId(agencyId, itemId);
        ContentServiceItemResponse res = new ContentServiceItemResponse(trackingId, itemEntities);
        // todo: exception handling?
        return Response.ok(res, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @GET
    @Path("holdings-by-branch")
    public Response getByBranch(@QueryParam("branch") String branchId,
                                @QueryParam("pid") List<String> pids,
                                @QueryParam("trackingId") @LogAs("trackingId") @GenerateTrackingId String trackingId) {

        if (branchId == null || branchId.isEmpty()) {
            log.error("holdings-by-branch called with no branch");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (pids == null || pids.isEmpty()) {
            log.error("holdings-by-branch called with no pids");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        log.debug("holdings-by-pid called with branch {},  pids: {}, trackingId: {}", branchId, pids, trackingId);
        HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, trackingId);
        Set<String> bibliographicIds = pids.stream().map(s -> s.replaceFirst("^\\d+-[a-z0-9]+:", "")).collect(toSet());

        try {
            List<CompleteItemFull> completeItems = bibliographicIds.stream()
                    .map(b -> dao.getItemsFromBranchIdAndBibliographicRecordId(branchId, b))
                    .flatMap(Collection::stream)
                    .map(CompleteItemFull::new)
                    .collect(toList());
            ContentServiceBranchResponse res = new ContentServiceBranchResponse(trackingId, completeItems);
            return Response.ok(res, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            log.error("Exception requesting for branch: {}: {}", branchId, e.getMessage());
            log.debug("Exception requesting for branch: {}: ", branchId, e);
            return Response.serverError().build();
        }
    }

    @GET
    @Path("holdings-by-pid")
    public Response getItemEntities(
            @QueryParam("agency") Integer agencyId,
            @QueryParam("pid") List<String> pids,
            @QueryParam("trackingId") @LogAs("trackingId") @GenerateTrackingId String trackingId) {
        { // argument validation
            if (agencyId == null || agencyId < 0) {
                log.error("holdings-by-pid called with no agency");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (!pids.stream().allMatch(s -> s.contains(":"))) {
                log.error("holdings-by-pid: All argument pids must contain at least one colon");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            List<String> bibliographicRecordIds = pids.stream().map(s -> s.split(":", 2)[1]).collect(Collectors.toList());
            HashSet<String> uniqueBibliographicRecordIds = new HashSet<>(bibliographicRecordIds);
            if (uniqueBibliographicRecordIds.size() < bibliographicRecordIds.size()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }
        log.debug("holdings-by-pid called with agency {}, pids: {}, trackingId: {}", agencyId, pids, trackingId);
        HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, trackingId);
        Map<String, String> pidMap = new HashMap<>();
        for (String p : pids) {
            pidMap.put(p, p.split(":", 2)[1]);
        }
        Map<String, Iterable<ItemEntity>> res = new HashMap<>();
        for (Map.Entry<String, String> e : pidMap.entrySet()) {
            Set<ItemEntity> pidItems = dao.getItemsFromAgencyAndBibliographicRecordId(agencyId, e.getValue());
            res.put(e.getKey(), pidItems);
        }
        return Response.ok(new ContentServicePidResponse(trackingId, res)).build();
    }

    @POST
    @Path("laesekompas-data-for-bibliographicrecordids")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getLaesekompasdataForBibliographicRecordIdsPost(
            String bibliographicRecordIds,
            @QueryParam("trackingId") @LogAs("trackingId") @GenerateTrackingId String trackingId
    ) {
        List<String> bibRecordIdList = null;
        try {
            bibRecordIdList = O.readValue(bibliographicRecordIds, List.class);
        } catch (JsonProcessingException e) {
            log.error("holdings-by-bibliographicrecordids: error parsing request body!");
            log.error(e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        log.debug("holdings-by-bibliographicrecordids called with trackingId {} and bibRecordId-list of length {}", trackingId, bibRecordIdList.size());
        HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, trackingId);
        Map<String, Iterable<LaesekompasHoldingsEntity>> res = new HashMap<>();
        for (String bibliographicRecordId : bibRecordIdList) {
            List<Object[]> laesekompasObjects = dao.getAgencyBranchStringsForBibliographicRecordId(bibliographicRecordId);
            List<LaesekompasHoldingsEntity> laesekompasHoldingsEntities =
                    laesekompasObjects.stream()
                            .map(oa -> LaesekompasHoldingsEntity.fromDatabaseObjects(oa))
                            .filter(lke -> lke != null && lke.status != Status.DECOMMISSIONED && !lke.branch.isEmpty())
                            .collect(Collectors.toList());
            res.put(bibliographicRecordId, laesekompasHoldingsEntities);
        }
        return Response.ok(new ContentServiceLaesekompasResponse(trackingId, res)).build();
    }

    @GET
    @Path("complete/{agencyId:\\d+}/{bibliographicRecordId}")
    public Response getComplete(@PathParam("agencyId") int agencyId,
                                @PathParam("bibliographicRecordId") String bibliographicRecordId,
                                @QueryParam("trackingId") @LogAs("trackingId") @GenerateTrackingId String trackingId) {
        try (LogWith l = LogWith.track(trackingId)) {
            l.agencyId(agencyId).bibliographicRecordId(bibliographicRecordId);

            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, trackingId);
            BibliographicItemEntity rec = dao.getRecordCollectionUnLocked(bibliographicRecordId, agencyId);
            if (rec == null) {
                log.info("Requested complete {}:{} Not found", agencyId, bibliographicRecordId);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            log.info("Requested complete {}:{}", agencyId, bibliographicRecordId);
            return Response.ok(new CompleteBibliographic(rec, trackingId)).build();
        }
    }

    @GET
    @Path("doc")
    public Response getDocumentation() {
        log.info("index.html");
        return Response.ok()
                .type(MediaType.TEXT_HTML_TYPE)
                .entity(indexHtml.getInputStream())
                .build();
    }

}
