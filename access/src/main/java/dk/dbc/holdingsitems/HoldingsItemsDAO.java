/*
 * Copyright (C) 2014-2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items
 *
 * holdings-items is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems;

import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.Status;
import dk.dbc.pgqueue.PreparedQueueSupplier;
import dk.dbc.pgqueue.QueueSupplier;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class HoldingsItemsDAO {

    private static final Logger log = LoggerFactory.getLogger(HoldingsItemsDAO.class);

    private static final QueueSupplier QUEUE_SUPPLIER = new QueueSupplier(QueueJob.STORAGE_ABSTRACTION);

    private static final String DATABASE_ERROR = "Database error";

    private final EntityManager em;
    private final LazyObject<Connection> connection;
    private final String trackingId;

    private final LazyObject<PreparedQueueSupplier> queueSupplier;

    private static class LazyObject<T> {

        private final Supplier<T> supplier;
        private T value;

        public LazyObject(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public synchronized T get() {
            if (value == null) {
                value = supplier.get();
                if (value == null)
                    throw new IllegalStateException("Got null value from supplier");
            }
            return value;
        }
    }

    private Connection supplyConnection() {
        return em.unwrap(Connection.class);
    }

    private PreparedQueueSupplier supplyQueueSupplier() {
        return QUEUE_SUPPLIER.preparedSupplier(connection.get());
    }

    /**
     * Constructor
     *
     * @param em         EntityManager
     * @param trackingId tracking of updates
     */
    HoldingsItemsDAO(EntityManager em, String trackingId) {
        this.em = em;
        this.connection = new LazyObject<>(this::supplyConnection);
        this.queueSupplier = new LazyObject<>(this::supplyQueueSupplier);
        this.trackingId = trackingId;
    }

    /**
     * Load a class and instantiate it based on the driver name from the
     * supplied connection
     *
     * @param em database connection
     * @return a HoldingsItemsDAO for the connection
     */
    public static HoldingsItemsDAO newInstance(EntityManager em) {
        return newInstance(em, "");
    }

    /**
     * Load a class and instantiate it based on the driver name from the
     * supplied connection
     *
     * @param em         database connection
     * @param trackingId tracking id for database updates
     * @return a HoldingsItemsDAO for the connection
     */
    public static HoldingsItemsDAO newInstance(EntityManager em, String trackingId) {
        return new HoldingsItemsDAO(em, trackingId);
    }

    /**
     * Get the tracking id
     *
     * @return The stored tracking id
     */
    public String getTrackingId() {
        return trackingId;
    }

    /**
     * Find all bibliographicrecordids that are not decommissioned, for a given
     * agency
     * <p>
     * This will be slow
     *
     * @param agencyId agency in question
     * @return collection of bibliographicrecordids for the given agency
     */
    public Set<String> getBibliographicIds(int agencyId) {
        return new HashSet<>(em.createQuery("SELECT h.bibliographicRecordId" +
                                            " FROM ItemEntity h" +
                                            " WHERE h.agencyId = :agencyId" +
                                            " GROUP BY h.agencyId, h.bibliographicRecordId",
                                            String.class)
                .setParameter("agencyId", agencyId)
                .getResultList());
    }

    /**
     * Find all bibliographicrecordids for a given agency
     * <p>
     * This will be slow
     *
     * @param agencyId agency in question
     * @return collection of bibliographicrecordids for the given agency
     */
    public Set<String> getBibliographicIdsIncludingDecommissioned(int agencyId) {
        return new HashSet<>(em.createQuery("SELECT h.bibliographicRecordId" +
                                            " FROM BibliographicItemEntity h" +
                                            " WHERE h.agencyId = :agencyId",
                                            String.class)
                .setParameter("agencyId", agencyId)
                .getResultList());
    }

    /**
     * Find all issueids for a given bibliographic record
     *
     * @param bibliographicId the bibliographic record
     * @param agencyId        the agency in question
     * @return a collection of all the issueids for the record
     * @throws HoldingsItemsException When database communication fails
     */
    public Set<String> getIssueIds(String bibliographicId, int agencyId) throws HoldingsItemsException {
        List<String> list = em.createQuery(
                "SELECT h.issueId" +
                " FROM IssueEntity h" +
                " WHERE h.agencyId = :agencyId" +
                "  AND h.bibliographicRecordId = :bibliographicRecordId",
                String.class)
                .setParameter("agencyId", agencyId)
                .setParameter("bibliographicRecordId", bibliographicId)
                .getResultList();
        return new HashSet<>(list);
    }

    /**
     * Find all item entities that match the given agencyId and itemId.
     * Normally there will be at most one, but there can be more if the same
     * item id is connected to the same
     * issueId / bibliographicRecordId. That is normally a data problem.
     *
     * @param agencyId id of library in question
     * @param itemId   item id to search for
     * @return a collection of ItemEntity objects that match the parameters.
     */
    public Set<ItemEntity> getItemsFromAgencyIdAndItemId(int agencyId, String itemId) {
        List<ItemEntity> itemList =
                em.createQuery("SELECT h" +
                               " FROM ItemEntity h" +
                               " WHERE h.agencyId = :agencyId" +
                               "  AND h.itemId = :itemId",
                               ItemEntity.class)
                        .setParameter("agencyId", agencyId)
                        .setParameter("itemId", itemId)
                        .getResultList();
        return new HashSet<>(itemList);
    }

    /**
     * Find all issue entities that match the given agencyId and item id.
     *
     * @param agencyId              id of agency in question
     * @param bibliographicRecordId
     * @return the issue entities that match the provided arguments.
     */
    private Set<IssueEntity> getIssuesFromAgencyAndBibliographicRecordId(int agencyId, String bibliographicRecordId) {
        List<IssueEntity> issueList =
                em.createQuery("SELECT h" +
                               " FROM IssueEntity h" +
                               " WHERE h.agencyId = :agencyId" +
                               "  AND h.bibliographicRecordId = :bibliographicRecordId",
                               IssueEntity.class)
                        .setParameter("agencyId", agencyId)
                        .setParameter("bibliographicRecordId", bibliographicRecordId)
                        .getResultList();
        return new HashSet<>(issueList);
    }

    /**
     * Get all holding items that match a given combination of agencyId,
     * bibliographicRecordId, and issueId
     *
     * @param agencyId              id of a library (int)
     * @param bibliographicRecordId bibliographic record id (string)
     * @param issueId               issue id (string)
     * @return the set of holding item entities that match the arguments.
     */
    private Set<ItemEntity> getItemsFromAgencyAndBibiliographicRecordIdAndIssueId(int agencyId, String bibliographicRecordId, String issueId) {
        List<ItemEntity> itemList =
                em.createQuery("SELECT h FROM ItemEntity h" +
                               " WHERE h.agencyId = :agencyId" +
                               "  AND h.bibliographicRecordId = :bibliographicRecordId" +
                               "  AND h.issueId = :issueId",
                               ItemEntity.class)
                        .setParameter("agencyId", agencyId)
                        .setParameter("bibliographicRecordId", bibliographicRecordId)
                        .setParameter("issueId", issueId)
                        .getResultList();
        return new HashSet<>(itemList);
    }

    /**
     * Get all items that match a given combination of agencyId and
     * bibliographicRecordId
     *
     * @param agencyId              id of a library (int)
     * @param bibliographicRecordId id of a bibliographic record (string)
     * @return the set of holdings items that match the arguments.
     */
    public Set<ItemEntity> getItemsFromAgencyAndBibliographicRecordId(int agencyId, String bibliographicRecordId) {
        Set<ItemEntity> res = new HashSet<>();
        Set<IssueEntity> issues = getIssuesFromAgencyAndBibliographicRecordId(agencyId, bibliographicRecordId);
        issues.forEach(i -> res.addAll(getItemsFromAgencyAndBibiliographicRecordIdAndIssueId(agencyId, bibliographicRecordId, i.getIssueId())));
        return res;
    }

    /**
     * Get all items that match a given bibliographicRecordId (for læsekompas)
     *
     * @param bibliographicRecordId id of a bibliographic record (string)
     * @return the set of holdings items that match the arguments.
     */
    public Set<ItemEntity> getItemsFromBibliographicRecordId(String bibliographicRecordId) {
        List<ItemEntity> itemList =
                em.createQuery("SELECT h from ItemEntity h WHERE h.bibliographicRecordId = :bibliographicRecordId", ItemEntity.class)
                .setParameter("bibliographicRecordId", bibliographicRecordId)
                .getResultList();
        return new HashSet<>(itemList);
    }

    /**
     * Create / Get a collection of items defined by id/library/orderId
     *
     * @param bibliographicRecordId part of the primary key
     * @param agencyId              part of the primary key
     * @param modified              timestamp to use for created/complete if a
     *                              new record is created
     * @return record collection object
     */
    public BibliographicItemEntity getRecordCollection(String bibliographicRecordId, int agencyId, Instant modified) {
        BibliographicItemEntity b = BibliographicItemEntity.from(
                em, agencyId, bibliographicRecordId, modified,
                modified == null ? null : LocalDateTime.ofInstant(modified, ZoneOffset.UTC).toLocalDate());
        if (b.isNew())
            b.setTrackingId(trackingId);
        return b;
    }

    /**
     * Create / Get a collection of items defined by id/library/orderId
     *
     * @param bibliographicRecordId part of the primary key
     * @param agencyId              part of the primary key
     * @return record collection object null if non-existing
     */
    public BibliographicItemEntity getRecordCollectionUnLocked(String bibliographicRecordId, int agencyId) {
        return  BibliographicItemEntity.fromUnLocked(
                em, agencyId, bibliographicRecordId);
    }

    /**
     * Get a set of agencies that has holdings for a record
     *
     * @param bibliographicRecordId id of record
     * @return set of agencyId
     * @throws HoldingsItemsException When database communication fails
     */
    public Set<Integer> getAgenciesThatHasHoldingsFor(String bibliographicRecordId) throws HoldingsItemsException {
        List<Integer> list = em.createQuery(
                "SELECT h.agencyId FROM ItemEntity h" +
                " WHERE h.bibliographicRecordId = :bibId" +
                " GROUP BY h.agencyId",
                Integer.class)
                .setParameter("bibId", bibliographicRecordId)
                .getResultList();
        return new HashSet<>(list);
    }

    /**
     * Update bibliographic item note - should be called before items are
     * fetched
     *
     * @param note                  The text to apply to all issues
     * @param agencyId              owner
     * @param bibliographicRecordId owner
     * @param modified              modified timestamp
     * @throws HoldingsItemsException in case of a database error
     */
    public void updateBibliographicItemNote(String note, int agencyId, String bibliographicRecordId, Instant modified) throws HoldingsItemsException {
        BibliographicItemEntity item = BibliographicItemEntity.from(em, agencyId, bibliographicRecordId, modified, null);
        if (item.isNew() ||
            !item.getModified().isAfter(modified))
            item.setNote(note);
    }

    /**
     * Create a map of status to number of items with said status
     *
     * @param bibliographicRecordId key
     * @param agencyId              key
     * @return key-value pairs with status and number of that status
     * @throws HoldingsItemsException in case of a database error
     */
    public Map<Status, Long> getStatusFor(String bibliographicRecordId, int agencyId) throws HoldingsItemsException {
        return (Map<Status, Long>) em.createQuery(
                "SELECT new " + StatusDTO.class.getCanonicalName() + "(h.status, COUNT(h.status))" +
                " FROM ItemEntity h" +
                " WHERE h.agencyId = :agencyId" +
                "  AND h.bibliographicRecordId = :bibliographicRecordId" +
                " GROUP BY h.status",
                StatusDTO.class)
                .setParameter("agencyId", agencyId)
                .setParameter("bibliographicRecordId", bibliographicRecordId)
                .getResultStream()
                .collect(Collectors.toMap(StatusDTO::getStatus,
                                          StatusDTO::getCount));
    }

    private static class StatusDTO {

        private final Status status;
        private final long count;

        public StatusDTO(Status status, long count) {
            this.status = status;
            this.count = count;
        }

        public Status getStatus() {
            return status;
        }

        public long getCount() {
            return count;
        }

    }

    /**
     * Has a holding that is not decommissioned
     *
     * @param bibliographicRecordId key
     * @param agencyId              key
     * @return if any holding is not 'decommissioned'
     * @throws HoldingsItemsException in case of a database error
     */
    public boolean hasLiveHoldings(String bibliographicRecordId, int agencyId) throws HoldingsItemsException {
        return !em.createQuery(
                "SELECT h.status" +
                " FROM ItemEntity h" +
                " WHERE h.agencyId = :agencyId" +
                "  AND h.bibliographicRecordId = :bibliographicRecordId")
                .setParameter("agencyId", agencyId)
                .setParameter("bibliographicRecordId", bibliographicRecordId)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
    }

    /**
     * Put an element into the queue
     *
     * @param bibliographicRecordId part of job id
     * @param agencyId              part of job id
     * @param stateChange           part of a job
     * @param worker                Who's get the job
     * @throws HoldingsItemsException in case of a database error
     */
    public void enqueue(String bibliographicRecordId, int agencyId, String stateChange, String worker) throws HoldingsItemsException {
        PreparedQueueSupplier supplier = queueSupplier.get();
        try {
            supplier.enqueue(worker, new QueueJob(agencyId, bibliographicRecordId, stateChange, trackingId));
        } catch (SQLException ex) {
            log.error(DATABASE_ERROR, ex);
            throw new HoldingsItemsException(DATABASE_ERROR, ex);
        }
    }

    /**
     * Put an element into the queue
     *
     * @param bibliographicRecordId part of job id
     * @param agencyId              part of job id
     * @param stateChange           part of a job
     * @param worker                Who's get the job
     * @param milliSeconds          How many milliseconds from now it should be
     *                              dequeued at the earliest
     * @throws HoldingsItemsException in case of a database error
     */
    public void enqueue(String bibliographicRecordId, int agencyId, String stateChange, String worker, long milliSeconds) throws HoldingsItemsException {
        PreparedQueueSupplier supplier = queueSupplier.get();
        try {
            supplier.enqueue(worker, new QueueJob(agencyId, bibliographicRecordId, stateChange, trackingId), milliSeconds);
        } catch (SQLException ex) {
            log.error(DATABASE_ERROR, ex);
            throw new HoldingsItemsException(DATABASE_ERROR, ex);
        }
    }

}
