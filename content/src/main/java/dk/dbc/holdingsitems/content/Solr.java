package dk.dbc.holdingsitems.content;

import dk.dbc.holdingsitems.content.solr.SolrHoldingsResponse;
import dk.dbc.holdingsitems.content.solr.SolrHandler;
import java.io.IOException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    @Path("all-holdings/{agencyId : \\d+}")
    @Produces({MediaType.APPLICATION_JSON})
    @Timed
    public Response getStatus(@PathParam("agencyId") int agencyId) {
        try {
            log.info("Solr endpoint called for agencyId: {}", agencyId);
            SolrHoldingsResponse response = new SolrHoldingsResponse();
            solrHandler.loadAdencyHoldings(agencyId, response::consume);
            return Response.ok(response).build();
        } catch (IOException ex) {
            log.error("Error fetching data from SolR: {}", ex.getMessage());
            log.debug("Error fetching data from SolR: ", ex);
            return Response.serverError().build();
        }
    }
}
