package dk.dbc.holdingsitems.update;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

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
