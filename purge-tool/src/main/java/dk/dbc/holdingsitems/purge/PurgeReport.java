/*
This file is part of opensearch.
Copyright © 2013, Dansk Bibliotekscenter a/s,
Tempovej 7-11, DK-2750 Ballerup, Denmark. CVR: 15149043

opensearch is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

opensearch is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with opensearch.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.purge;

import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.jpa.HoldingsItemsStatus;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class PurgeReport {

    private static final Logger log = LoggerFactory.getLogger(PurgeReport.class);

    private final int agencyId;
    private final HoldingsItemsDAO dao;

    /**
     * Create the purge request
     *
     * @param dao      data access object for database
     * @param agencyId The agency ID to be processed
     */
    public PurgeReport(HoldingsItemsDAO dao, int agencyId) {
        this.dao = dao;
        this.agencyId = agencyId;
    }

    /**
     * Print list statistics for agency. E.g. before and after purging
     *
     * @throws HoldingsItemsException if DAO threw error
     */
    public void statusReport() throws HoldingsItemsException {
        Set<String> bibliographicIds = dao.getBibliographicIds(agencyId);
        statusReport(bibliographicIds);
    }

    public void statusReport(Set<String> bibliographicIds) throws HoldingsItemsException {
        Map<HoldingsItemsStatus, AtomicLong> allStatus = new HashMap<>();
        System.out.println("Status Report");
        System.out.printf("Found %9d Bibliographic Ids%n", bibliographicIds.size());

        for (String bibliographicId : bibliographicIds) {
            log.debug("Has {} - {}", agencyId, bibliographicId);
            Map<HoldingsItemsStatus, Long> statusFor = dao.getStatusFor(bibliographicId, agencyId);
            for (Map.Entry<HoldingsItemsStatus, Long> entry : statusFor.entrySet()) {
                HoldingsItemsStatus key = entry.getKey();
                allStatus.computeIfAbsent(key, k -> new AtomicLong(0));
                allStatus.get(key).addAndGet(entry.getValue());
                log.trace("Has agency {} - id {}: type {}, count: {}", agencyId, bibliographicId, key, entry.getValue());
            }
        }

        int items = 0;
        for (Map.Entry<HoldingsItemsStatus, AtomicLong> entry : allStatus.entrySet()) {
            HoldingsItemsStatus key = entry.getKey();
            long value = entry.getValue().get();
            log.debug("Found {} {} items", key, value);
            System.out.printf("Found %9d %s items%n", value, key);

            items += value;
        }
        System.out.printf("Found %9s total items%n", items);
    }

}
