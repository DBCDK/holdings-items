package dk.dbc.holdingsitems.content;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("developer")
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
