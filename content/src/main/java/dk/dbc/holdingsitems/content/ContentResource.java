package dk.dbc.holdingsitems.content;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dk.dbc.commons.mdc.GenerateTrackingId;
import dk.dbc.commons.mdc.LogAs;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.content.response.AgenciesWithHoldingsResponse;
import dk.dbc.holdingsitems.content.response.StatusCountResponse;
import dk.dbc.holdingsitems.content_dto.CompleteBibliographic;
import dk.dbc.holdingsitems.content.response.CompleteItemFull;
import dk.dbc.holdingsitems.content.response.ContentServiceBranchResponse;
import dk.dbc.holdingsitems.content.response.ContentServiceItemResponse;
import dk.dbc.holdingsitems.content.response.ContentServiceLaesekompasResponse;
import dk.dbc.holdingsitems.content.response.ContentServicePidResponse;
import dk.dbc.holdingsitems.content.response.IndexHtml;
import dk.dbc.holdingsitems.content.response.LaesekompasHoldingsEntity;

import dk.dbc.holdingsitems.TotalStatusCountsForAgency;
import dk.dbc.holdingsitems.jpa.BibliographicItemDetached;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.Status;
import dk.dbc.holdingsitems.jpa.SupersedesEntity;
import dk.dbc.log.LogWith;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Stateless
@Path("/")
@Produces("application/json")
public class ContentResource {

    private static final Logger log = LoggerFactory.getLogger(ContentResource.class);

    @Inject
    public EntityManager em;

    @Inject
    public IndexHtml indexHtml;
    private static final ObjectMapper O = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @GET
    @Path("agencies-with-holdings/{bibliographicRecordId}")
    @Produces({MediaType.APPLICATION_JSON})
    @Timed
    public Response agenciesWithHoldings(@PathParam("bibliographicRecordId") String bibliographicRecordId,
                                         @QueryParam("trackingId") @LogAs("trackingId") @GenerateTrackingId String trackingId) {
        try (LogWith l = LogWith.track(trackingId)) {
            l.bibliographicRecordId(bibliographicRecordId);

            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, trackingId);
            try {
                Set<Integer> agencies = new HashSet<>();
                agencies.addAll(dao.getAgenciesThatHasHoldingsFor(bibliographicRecordId));
                for (String superseded : SupersedesEntity.bySupersedingNoLock(em, bibliographicRecordId)
                        .map(SupersedesEntity::getSuperseded)
                        .collect(toList())) {
                    agencies.addAll(dao.getAgenciesThatHasHoldingsFor(superseded));
                }
                AgenciesWithHoldingsResponse resp = new AgenciesWithHoldingsResponse(agencies, trackingId);
                return Response.ok(resp).build();
            } catch (HoldingsItemsException e) {
                log.error("Exception requesting for bibliographicRecordId: {}: {}", bibliographicRecordId, e.getMessage());
                log.debug("Exception requesting for bibliographicRecordId: {}: ", bibliographicRecordId, e);
                return Response.serverError().build();
            }
        }
    }

    @GET
    @Path("holdings-per-status/{agencyId}")
    @Produces({MediaType.APPLICATION_JSON})
    @Timed
    public Response holdingsPerStatusByAgency(@PathParam("agencyId") Integer agencyId,
                                              @QueryParam("trackingId") @LogAs("trackingId") @GenerateTrackingId String trackingId) {

        if (agencyId == null || agencyId < 0) {
            log.error("holdings-per-status called with no agency");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try (LogWith l = LogWith.track(trackingId)) {
            l.agencyId(agencyId);

            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, trackingId);
            try {
                TotalStatusCountsForAgency resp = dao.getStatusCountsByAgency(agencyId, trackingId);
                return Response.ok(
                                       new StatusCountResponse(resp.getAgencyId(), resp.getStatusCounts(), resp.getTrackingId()))
                               .build();
            } catch (HoldingsItemsException e) {
                log.error("Exception requesting for agencyId: {}: {}", agencyId, e.getMessage());
                log.debug("Exception requesting for agencyId: {}: ", agencyId, e);
                return Response.serverError().build();
            }
        }
    }

    @GET
    @Path("holdings-by-agency-id/{agencyId}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getHoldingsItems(@PathParam("agencyId") int agencyId,
                                     @QueryParam("trackingId") @LogAs("trackingId") @GenerateTrackingId String trackingId) {

        HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, trackingId);
        Collection<String> holdingItems = dao.getHoldingItems(agencyId);
        StreamingOutput streamingOutput = outputStream -> {
            PrintStream stream = new PrintStream(outputStream, false, StandardCharsets.UTF_8);
            for (String holdingItem : holdingItems) {
                stream.println(holdingItem);
            }
        };
        return Response.ok(streamingOutput).build();
    }

    @GET
    @Path("holdings-by-item-id")
    @Produces({MediaType.APPLICATION_JSON})
    @Timed
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
    @Produces({MediaType.APPLICATION_JSON})
    @Timed
    public Response getByBranch(@QueryParam("agencyId") Integer agencyId,
                                @QueryParam("branchId") String branchId,
                                @QueryParam("pid") List<String> pids,
                                @QueryParam("trackingId") @LogAs("trackingId") @GenerateTrackingId String trackingId) {

        if (agencyId == null || agencyId == 0) {
            log.error("holdings-by-branch called with no agency");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
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
                    .map(b -> dao.getItemsFromBranchIdAndBibliographicRecordId(agencyId, branchId, b))
                    .flatMap(Collection::stream)
                    .map(CompleteItemFull::from)
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
    @Produces({MediaType.APPLICATION_JSON})
    @Timed
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
            List<String> bibliographicRecordIds = pids.stream().map(s -> s.split(":", 2)[1]).collect(toList());
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
    @Timed
    public Response getLaesekompasdataForBibliographicRecordIdsPost(
            String bibliographicRecordIds,
            @QueryParam("trackingId") @LogAs("trackingId") @GenerateTrackingId String trackingId) {
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
                            .map(LaesekompasHoldingsEntity::fromDatabaseObjects)
                            .filter(lke -> lke != null && lke.status != Status.DECOMMISSIONED && !lke.branch.isEmpty())
                            .collect(toList());
            res.put(bibliographicRecordId, laesekompasHoldingsEntities);
        }
        return Response.ok(new ContentServiceLaesekompasResponse(trackingId, res)).build();
    }

    @GET
    @Path("complete/{agencyId:\\d+}/{bibliographicRecordId}")
    @Produces({MediaType.APPLICATION_JSON})
    @Timed
    public Response getComplete(@PathParam("agencyId") int agencyId,
                                @PathParam("bibliographicRecordId") String bibliographicRecordId,
                                @QueryParam("trackingId") @LogAs("trackingId") @GenerateTrackingId String trackingId) {
        try (LogWith l = LogWith.track(trackingId)) {
            l.agencyId(agencyId).bibliographicRecordId(bibliographicRecordId);

            BibliographicItemDetached detached = BibliographicItemEntity.detachedWithSuperseded(em, agencyId, bibliographicRecordId);

            if (detached == null) {
                log.info("Requested complete {}:{} Not found", agencyId, bibliographicRecordId);
                return Response.status(Response.Status.NOT_FOUND).header("X-DBC-Status", "200 OK").build();
            }

            log.info("Requested complete {}:{}", agencyId, bibliographicRecordId);
            CompleteBibliographic resp = detached.toCompleteBibliographic();
            resp.trackingId = trackingId;
            return Response.ok(resp).build();
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
