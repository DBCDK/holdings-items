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

import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.jpa.Status;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import org.eclipse.persistence.jpa.config.QueryHint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class PurgeReport {

    private static final Logger log = LoggerFactory.getLogger(PurgeReport.class);

    /**
     * Print list statistics for agency.E.g.before and after purging
     *
     * @param agencyId
     * @param em
     * @return
     * @throws HoldingsItemsException if DAO threw error
     */
    public static HashSet<String> statusReport(int agencyId, EntityManager em) throws HoldingsItemsException {

        HashSet<String> bibIdsWithHoldings = new HashSet<>();
        HashSet<String> bibIdsWithoutHoldings = new HashSet<>();
        Map<Status, AtomicLong> allStatus = new HashMap<>();

        em.createNativeQuery("SELECT bibliographicRecordId, status FROM item WHERE agencyId=?")
                .setParameter(1, agencyId)
                .setHint(QueryHints.CACHE_STORE_MODE, CacheStoreMode.BYPASS)
                .setHint(QueryHints.MAINTAIN_CACHE, HintValues.FALSE)
                .getResultStream()
                .forEach(os -> {
                    String bibliographicRecordId = ((String) ( (Object[]) os )[0]);
                    Status status = Status.parse((String) ( (Object[]) os )[1]);
                    bibIdsWithHoldings.add(bibliographicRecordId);
                    allStatus.computeIfAbsent(status, s -> new AtomicLong(0))
                            .incrementAndGet();
                });
        em.createNativeQuery("SELECT bibliographicRecordId FROM bibliographicItem WHERE agencyId=?")
                .setParameter(1, agencyId)
                .setHint(QueryHints.CACHE_STORE_MODE, CacheStoreMode.BYPASS)
                .setHint(QueryHints.MAINTAIN_CACHE, HintValues.FALSE)
                .getResultStream()
                .forEach(os -> {
                    String bibliographicRecordId = (String) os;
                    if (!bibIdsWithHoldings.contains(bibliographicRecordId))
                        bibIdsWithoutHoldings.add(bibliographicRecordId);
                });

        System.out.println("Status Report");
        System.out.printf("Found %9d Bibliographic Ids with holdings%n", bibIdsWithHoldings.size());
        System.out.printf("Found %9d Bibliographic Ids without holdings%n", bibIdsWithoutHoldings.size());
        if (!bibIdsWithHoldings.isEmpty() && !bibIdsWithoutHoldings.isEmpty())
            System.out.printf("Found %9d Bibliographic Ids in total%n", bibIdsWithHoldings.size() + bibIdsWithoutHoldings.size());

        int items = 0;
        for (Map.Entry<Status, AtomicLong> entry : allStatus.entrySet()) {
            Status key = entry.getKey();
            long value = entry.getValue().get();
            log.debug("Found {} {} items", key, value);
            System.out.printf("Found %9d %s items%n", value, key);

            items += value;
        }
        System.out.printf("Found %9s items in total%n", items);

        // return total
        bibIdsWithHoldings.addAll(bibIdsWithoutHoldings);
        return bibIdsWithHoldings;
    }
}
