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

import dk.dbc.holdingsitems.monitor.monitor.Timed;
import dk.dbc.pgqueue.diags.QueueStatusBean;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

    private static final int DIAG_MAX_CACHE_AGE = 45;
    private static final int DIAG_PERCENT_MATCH = 90;
    private static final int DIAG_COLLAPSE_MAX_ROWS = 12500;

    @Resource(lookup = Config.DATABASE)
    DataSource dataSource;

    @EJB
    QueueStatusBean queueStatus;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Timed
    public Response getStatus() {
        log.info("getStatus called ");
        return Response.ok().entity("{ \"ok\": true }").build();
    }

    @GET
    @Path("queue")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getQueueStatus(@QueryParam("ignore") String ignore) {
        Set<String> ign = Collections.EMPTY_SET;
        if(ignore != null && ! ignore.isEmpty()) {
            ign = new HashSet<>(Arrays.asList(ignore.split(",")));
            log.debug("getQueueStatus(ign = {})", ign);
        }
        return queueStatus.getQueueStatus(dataSource, DIAG_MAX_CACHE_AGE, DIAG_PERCENT_MATCH, DIAG_COLLAPSE_MAX_ROWS, ign);
    }

    @GET
    @Path("diags")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getDiagDistribution(@QueryParam("zone") @DefaultValue("CET") String timeZoneName) {
        return queueStatus.getDiagDistribution(timeZoneName, dataSource, DIAG_PERCENT_MATCH, DIAG_COLLAPSE_MAX_ROWS);
    }

}
