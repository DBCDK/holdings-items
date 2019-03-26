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
import dk.dbc.holdingsitems.Record;
import dk.dbc.holdingsitems.RecordCollection;
import dk.dbc.openagency.client.OpenAgencyException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class Purge
{
    private static final Logger log = LoggerFactory.getLogger( Purge.class );
    private final String worker;
    private final String agencyName;
    private final int agencyId;

    private final int commitEvery;
    private final boolean dryRun;

    private final Connection connection;
    private final HoldingsItemsDAO dao;

    /**
     * Create the purge request
     * @param connection DB connection
     * @param dao data access object for database
     * @param worker Name of worker to put in queue
     * @param agencyName The name of the agency to verify against
     * @param agencyId The agency ID to be processed 
     * @param commitEvery Optionally commit in batches when purging large sets. 0 to disable option
     * @param dryRun Optionally check but does not commit anything
     */
    public Purge(Connection connection, HoldingsItemsDAO dao, String worker, String agencyName, int agencyId, int commitEvery, boolean dryRun) {
        log.debug("Purge for agency ID {} with Queue: '{}'", agencyId, worker);
        this.connection = connection;
        this.dao = dao;
        this.worker = worker;
        this.agencyName = agencyName;
        this.agencyId = agencyId;
        this.commitEvery = commitEvery;
        this.dryRun = dryRun;
    }

    /**
     * Process the purge
     * @throws HoldingsItemsException if DAO threw error
     * @throws SQLException in case of rollback or commit error
     */
    public void process() throws HoldingsItemsException, SQLException {
        log.debug("Process");

        long start = System.currentTimeMillis();
        
        Set<String> bibliographicIds = dao.getBibliographicIds(agencyId);
        int recordsCount = bibliographicIds.size();
        int purgeCount = 0;
        log.info("Found {} Bibliographic Ids for agency {}", recordsCount, agencyId);
        status(bibliographicIds);

        // Confirm agency to purge
        log.info("Agency: {}, Name: '{}'", agencyId, agencyName);
        
        boolean acceptable = false;
        while (!acceptable) {
            System.out.print("Enter Agency Name to confirm purge: ");
            Scanner scanner = new Scanner(System.in);
            String output = scanner.next();
            acceptable = agencyName.equals(output);
            log.debug("Response: {}", acceptable);
            if (! acceptable) {
                log.info("Error on '{}' != '{}'", output, agencyName);
            }
        }
        log.info("Purging {}: '{}' with queue worker: '{}'", agencyId, agencyName, worker);
        
        purgeCount = purge(bibliographicIds);

        long end = System.currentTimeMillis();
        long duration = (end - start)/1000;
        log.info( "Purged {} with {} live records in {} s.", recordsCount, purgeCount, duration );
        
        status(bibliographicIds);
    }

    /**
     * Print list statistics for agency. E.g. before and after purging
     * @param bibliographicIds IDs to be processed
     * @throws HoldingsItemsException if DAO threw error
     */
    private void status(Set<String> bibliographicIds) throws HoldingsItemsException {
        Map<String, AtomicInteger> allStatus = new HashMap<>();
        
        for (String bibliographicId : bibliographicIds) {
            log.debug("Has {} - {}", agencyId, bibliographicId);
            Map<String, Integer> statusFor = dao.getStatusFor(bibliographicId, agencyId);
            for (Map.Entry<String, Integer> entry : statusFor.entrySet()) {
                String key = entry.getKey();
                allStatus.computeIfAbsent(key, k -> new AtomicInteger(0));
                allStatus.get(key).addAndGet(entry.getValue());
                log.trace("Has agency {} - id {}: type {}, count: {}", agencyId, bibliographicId, key, entry.getValue());
            }
        }
        
        int items = 0;
        for (Map.Entry<String, AtomicInteger> entry : allStatus.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue().get();
            log.info("Found {} items: {}", key, value);
            items += value;
        }
        log.info("Total {} items", items);
    }
    
    /**
     * Set records to 'decommissioned' and put on queue
     * @param bibliographicIds IDs to be processed
     * @return number of items that were processed
     * @throws HoldingsItemsException if DAO threw error
     * @throws SQLException in case of rollback or commit error
     */
    private int purge(Set<String> bibliographicIds) throws HoldingsItemsException, SQLException {
        log.trace("Purging {} records", bibliographicIds.size());
        int purge = 0;
        int records = 0;
        for (String bibliographicId : bibliographicIds) {
            log.trace("Bibliographic Id '{}'", bibliographicId);
            Set<String> issues = dao.getIssueIds(bibliographicId, agencyId);
            boolean toQueue = false;
            for (String issue : issues) {
                log.trace("Issue '{}'", issue);
                RecordCollection collection = dao.getRecordCollection(bibliographicId, agencyId, issue);
                for (Record record : collection) {
                    if (!record.getStatus().equals(Record.Decommissioned)) {
                        log.trace("Record {}", record);
                        record.setStatus(Record.Decommissioned);
                        records++;
                        toQueue = true;
                    }
                }
                // Save if any records were decommissioned
                collection.save();
            }
            if (toQueue) {
                dao.enqueue(bibliographicId, agencyId, Record.Decommissioned, worker);
            }
            
            purge++;
            if (commitEvery > 0 && purge % commitEvery == 0) {
                commit(purge);
            }
        }
        commit(purge);
        return records;
    }

    /**
     * Commit or rollback for database
     * @param count Count for log status
     * @throws SQLException 
     */
    private void commit(int count) throws SQLException {
        if (dryRun) {
            log.info("Rolled back at {}", count);
            connection.rollback();            
        } else {
            log.info("Commit at {}", count);
            connection.commit();
        }
    }
}
