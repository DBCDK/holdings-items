package dk.dbc.holdingsitems.content;

import dk.dbc.holdingsitems.content.response.ContentServiceItemResponse;
import dk.dbc.holdingsitems.content.response.ContentServicePidResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;

@Path("holdings-by-item-id")
@Produces("application/json")
public class ContentResource {
    private static final Logger log = LoggerFactory.getLogger(ContentResource.class);

    @GET
    public ContentServiceItemResponse getItemEntity(
            @QueryParam("agency") int agencyId,
            @QueryParam("itemId") String itemId,
            @QueryParam("trackingId") String trackingId)
    {
        return null;
    }

    @GET
    public ContentServicePidResponse getItemEntities(
            @QueryParam("agency") int agencyId,
            @QueryParam("pid") List<Integer> pids,
            @QueryParam("trackingId") String trackingId)
    {
        return null;
    }

}