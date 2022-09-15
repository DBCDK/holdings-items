package dk.dbc.holdingsitems.content;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

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
