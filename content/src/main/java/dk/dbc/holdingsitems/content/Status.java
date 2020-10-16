package dk.dbc.holdingsitems.content;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.eclipse.microprofile.metrics.annotation.Timed;

@Stateless
@Path("status")
public class Status {
    private static final Logger log = LoggerFactory.getLogger(Status.class);

    @Resource(lookup = "jdbc/holdings-items")
    DataSource dataSource;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Timed(reusable = true)
    public Response getStatus() {
        log.info("Status endpoint called.");
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery("SELECT 1")) {
            if (!resultSet.next())
                throw new SQLException("No rows returned in `SELECT 1'");
            return Response.ok().entity(new ResponseWrapper()).build();
        } catch (SQLException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ResponseWrapper(ex.getMessage())).build();
        }
    }

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static class ResponseWrapper {
        public boolean ok;
        public String text;

        public ResponseWrapper() {
            ok = true;
            text = "Success";
        }

        public ResponseWrapper(String diag) {
            ok = false;
            text = diag;
        }
    }

}
