package dk.dbc.holdingsitems.update;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("developer")
public class DeveloperResource {

    @PersistenceContext(unitName = "holdingsItems_PU")
    EntityManager em;

    @GET
    @Path("flush-entity-manager")
    public Response flush() {
        em.getEntityManagerFactory().getCache().evictAll();
        return Response.ok("ok").build();
    }
}
