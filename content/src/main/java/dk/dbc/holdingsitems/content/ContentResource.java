package dk.dbc.holdingsitems.content;

import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.RecordCollection;
import dk.dbc.holdingsitems.content.response.ContentServiceItemResponse;
import dk.dbc.holdingsitems.content.response.ContentServicePidResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Stateless
@Path("holdings-by-item-id")
@Produces("application/json")
public class ContentResource {

    @Resource(lookup = ContentServiceConfiguration.DATABASE)
    DataSource dataSource;

    private static final Logger log = LoggerFactory.getLogger(ContentResource.class);

    @GET
    public Response getItemEntity(
            @QueryParam("agency") int agencyId,
            @QueryParam("itemId") String itemId,
            @QueryParam("trackingId") String trackingId)
    {
        try (Connection connection = dataSource.getConnection()) {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, trackingId);
            RecordCollection recordCollection = dao.getRecordCollectionItemId(agencyId, itemId);
            ContentServiceItemResponse res = new ContentServiceItemResponse(trackingId, recordCollection);
            return Response.ok(res).build();
        } catch (HoldingsItemsException | SQLException ex) {
            log.error("Database issues: ", ex);
            return Response.serverError().build();
        }
    }

    @GET
    public Response getItemEntities(
            @QueryParam("agency") int agencyId,
            @QueryParam("pid") List<String> pids,
            @QueryParam("trackingId") String trackingId)
    {
        { // argument validation
            if (!(pids.stream().allMatch(s -> (s.contains(":") && s.chars().filter(ch -> ch == ':').count() == 1))))
                return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "All argument pids must contain exactly one colon").build();
            List<String> bibliographicRecordIds = pids.stream().map(s -> s.split(":")[1]).collect(Collectors.toList());
            HashSet<String> uniqueBibliographicRecordIds = new HashSet<String>(bibliographicRecordIds);
            if (uniqueBibliographicRecordIds.size() < bibliographicRecordIds.size()) {
                return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "BibliographicRecordIds in request pids must be unique").build();
            }
        }
        Map<String, String> pidMap = new HashMap<>();
        for (String p : pids) {
            pidMap.put(p, p.split(":")[1]);
        }
        Map<String, RecordCollection> res = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, trackingId);
            for (Map.Entry<String, String> e : pidMap.entrySet()) {
                RecordCollection collection = dao.getRecordCollectionPid(agencyId, e.getValue());
                res.put(e.getKey(), collection);
            }
            return Response.ok(new ContentServicePidResponse(trackingId, res)).build();
        } catch (SQLException | HoldingsItemsException ex) {
            log.error("Database issues: ", ex);
            return Response.serverError().build();
        }
    }

}