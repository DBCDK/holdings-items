/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items
 *
 * holdings-items is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.monitor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import javax.sql.DataSource;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Stateless
@Path("status")
public class Status {

    private static final Logger log = LoggerFactory.getLogger(Status.class);

    @Resource(lookup = "jdbc/holdings-items")
    DataSource dataSource;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Timed
    public Response getStatus() {
        log.info("getStatus called ");
        try (Connection connection = dataSource.getConnection() ;
             Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("SELECT 1")) {
            if (!resultSet.next())
                throw new SQLException("No rows returned in `SELECT 1'");
            return Response.ok().entity(new Resp()).build();
        } catch (SQLException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new Resp(ex.getMessage())).build();
        }
    }

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static class Resp {

        public boolean ok;
        public String text;

        public Resp() {
            ok = true;
            text = "Success";
        }

        public Resp(String diag) {
            ok = false;
            text = diag;
        }
    }
}
