package dk.dbc.holdingsitems.content.api.v1.update;

import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.log.LogWith;
import dk.dbc.oss.ns.holdingsitemsupdate.Authentication;
import dk.dbc.oss.ns.holdingsitemsupdate.BibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteHoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateResponse;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateResult;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateStatusEnum;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineBibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineHoldingsItemsUpdateRequest;
import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("v1/update")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UpdateV1 {

    private static final Logger log = LoggerFactory.getLogger(UpdateV1.class);

    @Inject
    UpdateV1Logic updateLogic;

    @Inject
    AccessValidator accessValidator;

    @POST
    @Path("completeHoldingsItemsUpdate")
    @Timed
    public Response completeHoldingsItemsUpdate(CompleteHoldingsItemsUpdateRequest req) {
        Authentication authentication = req.getAuthentication();
        req.setAuthentication(null);
        log.info("Complete {}", req);
        String trackingId = req.getTrackingId();
        if (trackingId == null) {
            trackingId = UUID.randomUUID().toString();
            req.setTrackingId(trackingId);
        }
        try (LogWith l = LogWith.track(trackingId)) {
            accessValidator.validate(authentication, req.getAgencyId());
            ensureRoot(req.getAgencyId(), Stream.of(req.getCompleteBibliographicItem().getBibliographicRecordId()));
            updateLogic.complete(req);
            return updateResponse(HoldingsItemsUpdateStatusEnum.OK, "ok");
        } catch (UpdateException ex) {
            log.error("Error validating user: {}", ex.getMessage());
            log.debug("Error validating user: ", ex);
            return updateResponse(ex);
        } catch (RuntimeException | HoldingsItemsException ex) {
            log.error("Error processing request: {}", ex.getMessage());
            log.debug("Error processing request: ", ex);
            return updateResponse(HoldingsItemsUpdateStatusEnum.FAILED_UPDATE_INTERNAL_ERROR, ex.getMessage());
        }
    }

    @POST
    @Path("holdingsItemsUpdate")
    @Timed
    public Response holdingsItemsUpdate(HoldingsItemsUpdateRequest req) {
        Authentication authentication = req.getAuthentication();
        req.setAuthentication(null);
        log.info("Update: {}", req);
        String trackingId = req.getTrackingId();
        if (trackingId == null) {
            trackingId = UUID.randomUUID().toString();
            req.setTrackingId(trackingId);
        }
        try (LogWith l = LogWith.track(trackingId)) {
            accessValidator.validate(authentication, req.getAgencyId());
            ensureRoot(req.getAgencyId(), req.getBibliographicItem().stream().map(BibliographicItem::getBibliographicRecordId));
            updateLogic.update(req);
            return updateResponse(HoldingsItemsUpdateStatusEnum.OK, "ok");
        } catch (UpdateException ex) {
            log.error("Error validating user: {}", ex.getMessage());
            log.debug("Error validating user: ", ex);
            return updateResponse(ex);
        } catch (RuntimeException | HoldingsItemsException ex) {
            log.error("Error processing request: {}", ex.getMessage());
            log.debug("Error processing request: ", ex);
            return updateResponse(HoldingsItemsUpdateStatusEnum.FAILED_UPDATE_INTERNAL_ERROR, ex.getMessage());
        }
    }

    @POST
    @Path("onlineHoldingsItemsUpdate")
    @Timed
    public Response onlineHoldingsItemsUpdate(OnlineHoldingsItemsUpdateRequest req) {
        Authentication authentication = req.getAuthentication();
        req.setAuthentication(null);
        log.info("Online: {}", req);
        String trackingId = req.getTrackingId();
        if (trackingId == null) {
            trackingId = UUID.randomUUID().toString();
            req.setTrackingId(trackingId);
        }
        try (LogWith l = LogWith.track(trackingId)) {
            accessValidator.validate(authentication, req.getAgencyId());
            ensureRoot(req.getAgencyId(), req.getOnlineBibliographicItem().stream().map(OnlineBibliographicItem::getBibliographicRecordId));
            updateLogic.online(req);
            return updateResponse(HoldingsItemsUpdateStatusEnum.OK, "ok");
        } catch (UpdateException ex) {
            log.error("Error validating user: {}", ex.getMessage());
            log.debug("Error validating user: ", ex);
            return updateResponse(ex);
        } catch (RuntimeException | HoldingsItemsException ex) {
            log.error("Error processing request: {}", ex.getMessage());
            log.debug("Error processing request: ", ex);
            return updateResponse(HoldingsItemsUpdateStatusEnum.FAILED_UPDATE_INTERNAL_ERROR, ex.getMessage());
        }
    }

    private static Response updateResponse(UpdateException ex) {
        return updateResponse(ex.getStatus(), ex.getMessage());
    }

    private static Response updateResponse(HoldingsItemsUpdateStatusEnum status, String message) {
        HoldingsItemsUpdateResponse resp = new HoldingsItemsUpdateResponse();
        HoldingsItemsUpdateResult res = new HoldingsItemsUpdateResult();
        resp.setHoldingsItemsUpdateResult(res);
        res.setHoldingsItemsUpdateStatus(status);
        res.setHoldingsItemsUpdateStatusMessage(message);
        return Response.ok(resp).build();
    }

    private void ensureRoot(int agencyId, Stream<String> bibliographicRecordIds) {
        try {
            updateLogic.ensureRoot(agencyId, bibliographicRecordIds);
        } catch (EJBException ex) {
            log.error("Error wnsuring root: {}", ex.getMessage());
            log.debug("Error wnsuring root: ", ex);
        }
    }
}
