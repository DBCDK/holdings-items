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
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.Status;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 *
 */
public class Purge {

    private static final Logger log = LoggerFactory.getLogger(Purge.class);
    private final String queue;
    private final String agencyName;
    private final int agencyId;

    private final boolean dryRun;

    private final EntityManager em;
    private final HoldingsItemsDAO dao;

    /**
     * Create the purge request
     *
     * @param em         Entity manager that is base of the dao
     * @param dao        data access object for database
     * @param queue      Name of worker to put in queue
     * @param agencyName The name of the agency to verify against
     * @param agencyId   The agency ID to be processed
     * @param dryRun     Optionally check but does not commit anything
     */
    public Purge(EntityManager em, HoldingsItemsDAO dao, String queue, String agencyName, int agencyId, boolean dryRun) {
        log.debug("Purge for agency ID {} with Queue: '{}'", agencyId, queue);
        this.em = em;
        this.dao = dao;
        this.queue = queue;
        this.agencyName = agencyName;
        this.agencyId = agencyId;
        this.dryRun = dryRun;
    }

    /**
     * Process the purge
     *
     * @throws HoldingsItemsException if DAO threw error
     * @throws SQLException           in case of rollback or commit error
     * @throws IOException            when waiting for human interaction
     */
    public void process() throws HoldingsItemsException, SQLException, IOException {
        log.debug("Process");

        long start = System.currentTimeMillis();

        Set<String> bibliographicIds = dao.getBibliographicIds(agencyId);
        int recordsCount = bibliographicIds.size();
        int purgeCount = 0;
        log.info("Found {} Bibliographic Ids for agency {}", recordsCount, agencyId);

        // Confirm agency to purge
        System.out.printf("Agency: %s, Name: '%s'%n", agencyId, agencyName);

        if (!userVerifyAgency())
            return;

        log.debug("Purging {}: '{}' with queue worker: '{}'", agencyId, agencyName, queue);
        System.out.printf("Purging %s: '%s' with queue worker: '%s'%n", agencyId, agencyName, queue);

        purgeCount = purge(bibliographicIds);

        long end = System.currentTimeMillis();
        long duration = ( end - start ) / 1000;
        log.info("Purged {} with {} live records in {} s.", recordsCount, purgeCount, duration);

    }

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    protected boolean userVerifyAgency() {
        for (;;) {
            System.out.print("Enter Agency Name to confirm purge (Enter to abort): ");
            Scanner scanner = new Scanner(System.in);
            String output = scanner.nextLine().trim();
            if (output.isEmpty())
                return false;
            boolean acceptable = agencyName.equals(output);
            log.debug("Response: {}", acceptable);
            if (acceptable) {
                break;
            } else {
                log.info("Error on '{}' != '{}'", output, agencyName);
            }
        }
        return true;
    }

    /**
     * Set records to 'decommissioned' and put on queue
     *
     * @param bibliographicIds IDs to be processed
     * @return number of items that were processed
     * @throws HoldingsItemsException if DAO threw error
     * @throws SQLException           in case of rollback or commit error
     */
    private int purge(Set<String> bibliographicIds) throws HoldingsItemsException, SQLException {
        log.trace("Purging {} records", bibliographicIds.size());
        AtomicInteger records = new AtomicInteger(0);
        for (String bibliographicId : bibliographicIds) {
            log.trace("Bibliographic Id '{}'", bibliographicId);
            boolean toQueue = false;
            BibliographicItemEntity bibItem = dao.getRecordCollection(bibliographicId, agencyId, null);
            List<ItemEntity> itemsToDecommission = bibItem.stream()
                    .flatMap(IssueEntity::stream)
                    .filter(item -> item.getStatus() != Status.DECOMMISSIONED)
                    .collect(toList());
            if (!itemsToDecommission.isEmpty()) {
                itemsToDecommission.forEach(ItemEntity::remove);
                toQueue = true;
            }
            if (!dryRun)
                bibItem.save();
            if (!dryRun && toQueue) {
                dao.enqueue(bibliographicId, agencyId, "{}", queue);
            }
        }

        return records.get();
    }

    /**
     * Cleanup the database
     *
     * @param removeFirstAcquisitionDate Remove all trace of the agency
     * @throws HoldingsItemsException If dao somehow fails
     */
    public void removeDecommissioned(boolean removeFirstAcquisitionDate) throws HoldingsItemsException {
        log.info("Removing records from database (wipe={})", removeFirstAcquisitionDate);
        if (dryRun)
            return;
        Instant now = Instant.now();
        dao.getBibliographicIdsIncludingDecommissioned(agencyId).stream()
                .map(bibliographicId -> dao.getRecordCollection(bibliographicId, agencyId, now))
                .forEach(removeFirstAcquisitionDate ?
                         em::remove :
                         bibItem -> {
                     bibItem.stream()
                             .collect(Collectors.toSet()).stream() // Hack to not modify container that is being iterated.
                             .forEach(bibItem::removeIssue);
                     bibItem.save();
                 });
    }

}
