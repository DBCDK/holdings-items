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

import dk.dbc.holdingsitems.EnqueueService;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.SupersedesEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 *
 */
public class Purge {

    private static final Logger log = LoggerFactory.getLogger(Purge.class);
    private final String supplier;
    private final String agencyName;
    private final int agencyId;

    private final boolean dryRun;
    private final boolean removeFirstAcquisitionDate;

    private final EntityManager em;
    private final HoldingsItemsDAO dao;

    /**
     * Create the purge request
     *
     * @param em                         Entity manager that is base of the dao
     * @param dao                        data access object for database
     * @param supplier                   Name(s) of worker that put in queue
     * @param agencyName                 The name of the agency to verify
     *                                   against
     * @param agencyId                   The agency ID to be processed
     * @param removeFirstAcquisitionDate if the bibliographic-item should be
     *                                   removed too
     * @param dryRun                     Optionally check but does not commit
     *                                   anything
     */
    public Purge(EntityManager em, HoldingsItemsDAO dao, String supplier, String agencyName, int agencyId, boolean removeFirstAcquisitionDate, boolean dryRun) {
        log.debug("Purge for agency ID {} with Queue: '{}'", agencyId, supplier);
        this.em = em;
        this.dao = dao;
        this.supplier = supplier;
        this.agencyName = agencyName;
        this.agencyId = agencyId;
        this.dryRun = dryRun;
        this.removeFirstAcquisitionDate = removeFirstAcquisitionDate;
    }

    /**
     * Process the purge
     *
     * @param bibliographicRecordIds the ids to clean up
     * @throws HoldingsItemsException if DAO threw error
     * @throws SQLException           in case of rollback or commit error
     * @throws IOException            when waiting for human interaction
     */
    public void process(Set<String> bibliographicRecordIds) throws HoldingsItemsException, SQLException, IOException {
        log.debug("Process");

        long start = System.currentTimeMillis();

        int recordsCount = bibliographicRecordIds.size();
        int purgeCount = 0;

        // Confirm agency to purge
        System.out.printf("Agency: %s, Name: '%s'%n", agencyId, agencyName);

        if (!userVerifyAgency()) {
            System.out.printf("Aborting...%n");
            return;
        }

        log.debug("Purging {}: '{}' with queue supplier: '{}'", agencyId, agencyName, supplier);
        System.out.printf("Purging %s: '%s' with queue supplier: '%s'%n", agencyId, agencyName, supplier);

        purgeCount = purge(bibliographicRecordIds);

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
        int purged = 0;
        try (EnqueueService enqueueService = dao.enqueueService()) {
            for (String bibliographicId : bibliographicIds) {
                log.trace("Bibliographic Id '{}'", bibliographicId);
                BibliographicItemEntity bibItem = dao.getRecordCollection(bibliographicId, agencyId, null);
                if (!dryRun) {
                    List<ItemEntity> items = bibItem.stream()
                            .flatMap(IssueEntity::stream)
                            .collect(toList());
                    if (removeFirstAcquisitionDate) {
                        em.remove(bibItem);
                        em.flush();
                    } else if (!items.isEmpty()) {
                        items.stream() // ConcurrentModificationException hack
                                .forEach(ItemEntity::remove);
                        bibItem.save(); // This removes issues too
                        em.flush();
                    }
                    em.detach(bibItem);
                    SupersedesEntity supersedes = em.find(SupersedesEntity.class, bibliographicId);
                    if (supersedes != null)
                        enqueueService.enqueue(supplier, agencyId, supersedes.getSuperseding());
                    enqueueService.enqueue(supplier, agencyId, bibliographicId);
                    purged++;
                }
            }
        }

        return purged;
    }

}
