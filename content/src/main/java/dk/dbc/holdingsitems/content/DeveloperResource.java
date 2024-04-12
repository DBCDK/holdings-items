package dk.dbc.holdingsitems.content;

import dk.dbc.commons.payara.helpers.DeveloperMode;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("developer")
@DeveloperMode
public class DeveloperResource {

    @Inject
    EntityManager em;

    @GET
    @Path("flush-entity-manager")
    public Response flush() {
        em.getEntityManagerFactory().getCache().evictAll();
        return Response.ok("ok").build();
    }
}
