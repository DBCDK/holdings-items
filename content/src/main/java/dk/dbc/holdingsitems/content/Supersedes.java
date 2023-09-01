package dk.dbc.holdingsitems.content;

import dk.dbc.commons.mdc.GenerateTrackingId;
import dk.dbc.commons.mdc.LogAs;
import dk.dbc.holdingsitems.EnqueueService;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.content.request.SupersedesRequest;
import dk.dbc.holdingsitems.content.response.StatusResponse;
import dk.dbc.holdingsitems.jpa.SupersedesEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
@Path("v1/supersedes")
public class Supersedes {

    private static final Logger log = LoggerFactory.getLogger(Supersedes.class);

    @Inject
    EntityManager em;

    @ConfigProperty(name = "SUPERSEDES_SUPPLIER", defaultValue = "SUPERSEDES")
    @Inject
    String supplier;

    @GET
    @Path("{faust}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("faust") @LogAs("faust") String faust,
                        @QueryParam("trackingId") @LogAs("TrackingId") @GenerateTrackingId String trackingId) {
        log.info("get({})", faust);
        List<SupersedesEntity> owned = SupersedesEntity.bySuperseding(em, faust);
        if (owned.isEmpty()) {
            return StatusResponse.notFound(trackingId, "No such record");
        }
        SupersedesRequest response = new SupersedesRequest();
        response.supersedes = owned.stream().map(SupersedesEntity::getSuperseded).collect(Collectors.toList());
        return Response.ok(response).build();
    }

    @DELETE
    @Path("{faust}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("faust") @LogAs("faust") String faust,
                           @QueryParam("trackingId") @LogAs("TrackingId") @GenerateTrackingId String trackingId) {
        log.info("delete({})", faust);
        List<SupersedesEntity> owned = SupersedesEntity.bySuperseding(em, faust);
        owned.forEach(em::remove);
        try {
            HashSet<String> fausts = new HashSet<>();
            fausts.add(faust);
            owned.forEach(e -> fausts.add(e.getSuperseded()));
            enqueue(fausts, trackingId);
        } catch (HoldingsItemsException ex) {
            return StatusResponse.error(trackingId, Response.Status.INTERNAL_SERVER_ERROR, "Error queuing: " + ex.getMessage());
        }
        return StatusResponse.ok(trackingId);
    }

    @PUT
    @Path("{faust}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(SupersedesRequest request,
                        @PathParam("faust") @LogAs("faust") String faust,
                        @QueryParam("trackingId") @LogAs("TrackingId") @GenerateTrackingId String trackingId) {
        log.info("put({})", faust);
        if (request.supersedes == null || request.supersedes.isEmpty()) {
            return StatusResponse.error(trackingId, Response.Status.BAD_REQUEST, "Invalid data");
        }

        boolean isOvertaken = em.find(SupersedesEntity.class, faust) != null; // Someone has overtaken this
        if (!isOvertaken) {
            request.supersedes.forEach(overtaken -> {
                SupersedesEntity entity = em.find(SupersedesEntity.class, overtaken);
                if (entity != null) {
                    entity.setSuperseding(faust);
                    em.merge(entity);
                } else {
                    entity = new SupersedesEntity(overtaken, faust);
                    em.persist(entity);
                }
            });

            try {
                HashSet<String> fausts = new HashSet<>();
                fausts.add(faust);
                fausts.addAll(request.supersedes);
                enqueue(fausts, trackingId);
            } catch (HoldingsItemsException ex) {
                return StatusResponse.error(trackingId, Response.Status.INTERNAL_SERVER_ERROR, "Error queuing: " + ex.getMessage());
            }
        }
        return StatusResponse.ok(trackingId);
    }

    /**
     * Enqueues for all agencies that has a holding on any related faust
     *
     *
     * @param fausts     All those who are affected by the change
     * @param trackingId Who performs the enqueue
     * @throws HoldingsItemsException If enqueue fails
     */
    private void enqueue(Iterable<String> fausts, String trackingId) throws HoldingsItemsException {
        HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, trackingId);
        Set<Integer> agencies = new HashSet<>();
        for (String faust : fausts) {
            agencies.addAll(dao.getAgenciesThatHasHoldingsFor(faust));
        }
        try (EnqueueService enqueueService = dao.enqueueService()) {
            for (String faust : fausts) {
                for (int agency : agencies) {
                    enqueueService.enqueue(supplier, agency, faust);
                }
            }
        }
    }
}
