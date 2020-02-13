package dk.dbc.holdingsitems.content;

import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.RecordCollection;
import dk.dbc.holdingsitems.content.response.ContentServiceItemResponse;
import dk.dbc.holdingsitems.content.response.ContentServicePidResponse;
import dk.dbc.holdingsitems.content.response.IndexHtml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Stateless
@Path("/")
@Produces("application/json")
public class ContentResource {

    @Resource(lookup = ContentServiceConfiguration.DATABASE)
    DataSource dataSource;

    @Inject
    public IndexHtml indexHtml;

    private static final Logger log = LoggerFactory.getLogger(ContentResource.class);

    @GET
    @Path("holdings-by-item-id")
    public Response getItemEntity(
            @QueryParam("agency") Integer agencyId,
            @QueryParam("itemId") String itemId,
            @QueryParam("trackingId") String trackingId) {
        { // argument validation
            if (agencyId == null || agencyId < 0) {
                log.error("holdings-by-item-id called with no agency");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (!(itemId.trim().length() > 0)) {
                log.error("holdings-by-item-id called with no item");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }
        log.debug("holdings-by-item-id called with agency: {}, itemId: {}, trackingId: {}", agencyId, itemId, trackingId);
        try (Connection connection = dataSource.getConnection()) {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, trackingId);
            RecordCollection recordCollection = dao.getRecordCollectionItemId(agencyId, itemId);
            ContentServiceItemResponse res = new ContentServiceItemResponse(trackingId, recordCollection);

            return Response.ok(res, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (HoldingsItemsException | SQLException ex) {
            log.error("Database issues: ", ex);
            return Response.serverError().build();
        }
    }

    @GET
    @Path("holdings-by-pid")
    public Response getItemEntities(
            @QueryParam("agency") Integer agencyId,
            @QueryParam("pid") List<String> pids,
            @QueryParam("trackingId") String trackingId) {
        { // argument validation
            if (agencyId == null || agencyId < 0) {
                log.error("holdings-by-pid called with no agency");
                return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).build();
            }
            if (!pids.stream().allMatch(s -> s.contains(":"))) {
                log.error("holdings-by-pid: All argument pids must contain at least one colon");
                return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).build();
            }
            List<String> bibliographicRecordIds = pids.stream().map(s -> s.split(":",2)[1]).collect(Collectors.toList());
            HashSet<String> uniqueBibliographicRecordIds = new HashSet<>(bibliographicRecordIds);
            if (uniqueBibliographicRecordIds.size() < bibliographicRecordIds.size()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }
        log.debug("holdings-by-pid called with agency {}, pids: {}, trackingId: {}", agencyId, pids, trackingId);
        Map<String, String> pidMap = new HashMap<>();
        for (String p : pids) {
            pidMap.put(p, p.split(":", 2)[1]);
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

    @GET
    @Path("doc")
    public Response getDocumentation() {
        log.info("index.html");
        return Response.ok()
                .type(MediaType.TEXT_HTML_TYPE)
                .entity(indexHtml.getInputStream())
                .build();
    }

}
