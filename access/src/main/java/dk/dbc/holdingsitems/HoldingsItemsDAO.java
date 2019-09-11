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

import dk.dbc.holdingsitems.jpa.HoldingsItemsCollectionEntity;
import dk.dbc.holdingsitems.jpa.HoldingsItemsStatus;
import dk.dbc.pgqueue.PreparedQueueSupplier;
import dk.dbc.pgqueue.QueueSupplier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
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
     * @param connection Database connection
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
     * @throws HoldingsItemsException if no database specific implementation
     *                                could be found
     */
    public static HoldingsItemsDAO newInstance(EntityManager em) throws HoldingsItemsException {
        return newInstance(em, "");
    }

    /**
     * Load a class and instantiate it based on the driver name from the
     * supplied connection
     *
     * @param em         database connection
     * @param trackingId tracking id for database updates
     * @return a HoldingsItemsDAO for the connection
     * @throws HoldingsItemsException if no database specific implementation
     *                                could be found
     */
    public static HoldingsItemsDAO newInstance(EntityManager em, String trackingId) throws HoldingsItemsException {
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
     * Find all bibliographicrecordids for a given agency
     * <p>
     * This will be slow
     *
     * @param agencyId agency in question
     * @return collection of bibliographicrecordids for the given agency
     */
    public Set<String> getBibliographicIds(int agencyId) throws HoldingsItemsException {
        return new HashSet<>(em.createQuery("SELECT h.bibliographicRecordId" + " FROM HoldingsItemsItemEntity h" + " WHERE h.agencyId = :agencyId" + "  AND h.status != :status" + " GROUP BY h.agencyId, h.bibliographicRecordId", String.class).setParameter("agencyId", agencyId).setParameter("status", HoldingsItemsStatus.DECOMMISSIONED).getResultList());
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
                " FROM HoldingsItemsCollectionEntity h" +
                " WHERE h.agencyId = :agencyId" +
                "  AND h.bibliographicRecordId = :bibliographicRecordId",
                String.class)
                .setParameter("agencyId", agencyId)
                .setParameter("bibliographicRecordId", bibliographicId)
                .getResultList();
        return new HashSet<>(list);
    }

    /**
     * Create / Get a collection of items defined by id/library/orderId
     *
     * @param bibliographicRecordId part of the primary key
     * @param agencyId              part of the primary key
     * @param issueId               part of the primary key
     * @return record collection object
     * @throws HoldingsItemsException When database communication fails
     */
    public HoldingsItemsCollectionEntity getRecordCollection(String bibliographicRecordId, int agencyId, String issueId, Instant modified) throws HoldingsItemsException {
        return HoldingsItemsCollectionEntity.from(em, agencyId, bibliographicRecordId, issueId, modified);
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
                "SELECT h.agencyId FROM HoldingsItemsItemEntity h" +
                " WHERE h.bibliographicRecordId = :bibId" +
                "  AND h.status != :status" +
                " GROUP BY h.agencyId",
                Integer.class)
                .setParameter("bibId", bibliographicRecordId)
                .setParameter("status", HoldingsItemsStatus.DECOMMISSIONED)
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
        HoldingsItemsCollectionEntity.byAgencyBibliographic(em, agencyId, bibliographicRecordId)
                .stream()
                .filter(e -> !e.getModified().isAfter(modified))
                .forEach(e -> {
                    e.setNote(note);
                    e.save();
                });
    }

    /**
     * Create a map of status to number of items with said status
     *
     * @param bibliographicRecordId key
     * @param agencyId              key
     * @return key-value pairs with status and number of that status
     * @throws HoldingsItemsException in case of a database error
     */
    public Map<HoldingsItemsStatus, Long> getStatusFor(String bibliographicRecordId, int agencyId) throws HoldingsItemsException {
        return (Map<HoldingsItemsStatus, Long>) em.createQuery(
                "SELECT h.status, COUNT(h.status)" +
                " FROM HoldingsItemsItemEntity h" +
                " WHERE h.agencyId = :agencyId" +
                "  AND h.bibliographicRecordId = :bibliographicRecordId" +
                " GROUP BY h.status")
                .setParameter("agencyId", agencyId)
                .setParameter("bibliographicRecordId", bibliographicRecordId)
                .getResultStream()
                .collect(Collectors.toMap(o -> nthAs(o, 0, HoldingsItemsStatus.class),
                                          o -> nthAs(o, 1, Long.class)));
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
                " FROM HoldingsItemsItemEntity h" +
                " WHERE h.agencyId = :agencyId" +
                "  AND h.bibliographicRecordId = :bibliographicRecordId" +
                "  AND h.status != :status")
                .setParameter("agencyId", agencyId)
                .setParameter("bibliographicRecordId", bibliographicRecordId)
                .setParameter("status", HoldingsItemsStatus.DECOMMISSIONED)
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

    /**
     * Put an element into the queue
     *
     * @param bibliographicRecordId part of job id
     * @param agencyId              part of job id
     * @param worker                Who's get the job
     * @throws HoldingsItemsException in case of a database error
     */
    public void enqueueOld(String bibliographicRecordId, int agencyId, String worker) throws HoldingsItemsException {
        enqueueOld(bibliographicRecordId, agencyId, worker, 0);
    }

    /**
     * Put an element into the queue
     *
     * @param bibliographicRecordId part of job id
     * @param agencyId              part of job id
     * @param worker                Who's get the job
     * @param milliSeconds          How many milliseconds from now it should be
     *                              dequeued at the earliest
     * @throws HoldingsItemsException in case of a database error
     */
    public void enqueueOld(String bibliographicRecordId, int agencyId, String worker, long milliSeconds) throws HoldingsItemsException {
        enqueueOld(bibliographicRecordId, agencyId, "", worker, milliSeconds);
    }

    /**
     * Put an element into the queue
     *
     * @param bibliographicRecordId part of job id
     * @param agencyId              part of job id
     * @param additionalData        extra data for job
     * @param worker                Who's to get the job
     * @throws HoldingsItemsException in case of a database error
     */
    public void enqueueOld(String bibliographicRecordId, int agencyId, String additionalData, String worker) throws HoldingsItemsException {
        enqueueOld(bibliographicRecordId, agencyId, worker, 0);
    }

    /**
     * Put an element into the queue
     *
     * @param bibliographicRecordId part of job id
     * @param agencyId              part of job id
     * @param additionalData        extra data for job
     * @param worker                Who's to get the job
     * @param milliSeconds          How many milliseconds from now it should be
     *                              dequeued at the earliest
     * @throws HoldingsItemsException in case of a database error
     */
    public void enqueueOld(String bibliographicRecordId, int agencyId, String additionalData, String worker, long milliSeconds) throws HoldingsItemsException {
        try (PreparedStatement stmt = connection.get().prepareStatement(QUEUE_INSERT_SQL_OLD)) {
            int i = 0;
            stmt.setString(++i, worker);
            stmt.setLong(++i, milliSeconds);
            stmt.setString(++i, bibliographicRecordId);
            stmt.setInt(++i, agencyId);
            stmt.setString(++i, additionalData);
            stmt.setString(++i, trackingId);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            log.error(DATABASE_ERROR, ex);
            throw new HoldingsItemsException(DATABASE_ERROR, ex);
        }
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static <T> T nthAs(Object o, int n, Class<T> clazz) {
        return (T) ( (Object[]) o )[n];
    }

    private static final String DATABASE_ERROR = "Database error";
    private static final String QUEUE_INSERT_SQL_OLD = "INSERT INTO q(worker, queued, bibliographicRecordId, agencyId, additionalData, trackingId) VALUES(?, clock_timestamp() + ? * INTERVAL '1 milliseconds', ?, ?, ?, ?)";

}
