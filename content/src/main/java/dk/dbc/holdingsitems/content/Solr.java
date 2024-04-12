package dk.dbc.holdingsitems.content;

import dk.dbc.commons.payara.helpers.GenerateTrackingId;
import dk.dbc.commons.payara.helpers.LogAs;
import dk.dbc.holdingsitems.content.solr.SolrHoldingsResponse;
import dk.dbc.holdingsitems.content.solr.SolrHandler;
import dk.dbc.holdingsitems.content.solr.SolrPidResponse;
import java.io.IOException;
import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
@Path("solr")
public class Solr {

    private static final Logger log = LoggerFactory.getLogger(Solr.class);

    @Inject
    SolrHandler solrHandler;

    @GET
    @Path("all-holdings")
    @Produces({MediaType.APPLICATION_JSON})
    @Timed
    public Response getStatus(@QueryParam("agencyId") Integer agencyId,
                              @QueryParam("trackingId") @LogAs("trackingId") @GenerateTrackingId String trackingId) {
        log.info("all-holdings({})", agencyId);
        if (agencyId == null || agencyId == 0) {
            log.error("all-holdings called with no agency");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        log.debug("solr/all-holdings called with agencyId: {}, trackingId: {}", agencyId, trackingId);

        try {
            SolrHoldingsResponse response = new SolrHoldingsResponse(trackingId);
            solrHandler.loadAgencyHoldings(agencyId, response::consume);
            return Response.ok(response).build();
        } catch (IOException ex) {
            log.error("Error fetching data from SolR: {}", ex.getMessage());
            log.debug("Error fetching data from SolR: ", ex);
            return Response.serverError().build();
        }
    }

    @GET
    @Path("status-by-pid")
    @Produces({MediaType.APPLICATION_JSON})
    @Timed
    public Response getStatusByPid(@QueryParam("agencyId") Integer agencyId,
                                   @QueryParam("pid") List<String> pids,
                                   @QueryParam("trackingId") @LogAs("trackingId") @GenerateTrackingId String trackingId) {
        log.info("status-by-pid({}, {})", agencyId, pids);
        if (agencyId == null || agencyId == 0) {
            log.error("status-by-pid called with no agency");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (pids == null || pids.isEmpty()) {
            log.error("status-by-pid called with no pids");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        log.debug("solr/status-by-pid called with agencyId: {}, pids: {}, trackingId: {}", agencyId, pids, trackingId);

        try {
            SolrPidResponse response = new SolrPidResponse(trackingId);
            for (String pid : pids) {
                solrHandler.loadPidHoldings(agencyId, pid, response.consumerFor(pid));
            }
            return Response.ok(response).build();
        } catch (IOException ex) {
            log.error("Error fetching data from SolR: {}", ex.getMessage());
            log.debug("Error fetching data from SolR: ", ex);
            return Response.serverError().build();
        }
    }
}
