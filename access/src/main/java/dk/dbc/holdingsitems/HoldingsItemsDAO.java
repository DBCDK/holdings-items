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

import dk.dbc.pgqueue.PreparedQueueSupplier;
import dk.dbc.pgqueue.QueueSupplier;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Date;
import java.util.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class HoldingsItemsDAO {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HoldingsItemsDAO.class);

    private static final QueueSupplier QUEUE_SUPPLIER = new QueueSupplier(QueueJob.STORAGE_ABSTRACTION);

    private static final int SCHEMA_VERSION = 14;

    private final Connection connection;
    private final String trackingId;

    private PreparedQueueSupplier queueSupplier = null;

    private PreparedQueueSupplier getQueueSupplier() {
        if (queueSupplier == null) {
            queueSupplier = QUEUE_SUPPLIER.preparedSupplier(connection);
        }
        return queueSupplier;
    }

    /**
     * Constructor
     *
     * @param connection Database connection
     * @param trackingId tracking of updates
     */
    HoldingsItemsDAO(Connection connection, String trackingId) {
        this.connection = connection;
        this.trackingId = trackingId;
    }

    /**
     * Load a class and instantiate it based on the driver name from the
     * supplied connection
     *
     * @param connection database connection
     * @return a HoldingsItemsDAO for the connection
     * @throws HoldingsItemsException if no database specific implementation
     *                                could be found
     */
    public static HoldingsItemsDAO newInstance(Connection connection) throws HoldingsItemsException {
        return newInstance(connection, "");
    }

    /**
     * Load a class and instantiate it based on the driver name from the
     * supplied connection
     *
     * @param connection database connection
     * @param trackingId tracking id for database updates
     * @return a HoldingsItemsDAO for the connection
     * @throws HoldingsItemsException if no database specific implementation
     *                                could be found
     */
    public static HoldingsItemsDAO newInstance(Connection connection, String trackingId) throws HoldingsItemsException {
        return newInstance(connection, trackingId, false);
    }

    /**
     * Load a class and instantiate it based on the driver name from the
     * supplied connection
     *
     * @param connection     database connection
     * @param trackingId     tracking id for database updates
     * @param skipValidation don't validate the connection
     * @return a HoldingsItemsDAO for the connection
     * @throws HoldingsItemsException if no database specific implementation
     *                                could be found
     */
    public static HoldingsItemsDAO newInstance(Connection connection, String trackingId, boolean skipValidation) throws HoldingsItemsException {
        HoldingsItemsDAO dao = new HoldingsItemsDAO(connection, trackingId);
        if (!skipValidation) {
            dao.validateConnection();
        }
        return dao;
    }

    /**
     *
     * @throws HoldingsItemsException if schema is in-compatible
     */
    protected void validateConnection() throws HoldingsItemsException {
        try {
            try (PreparedStatement stmt = connection.prepareStatement(VALIDATE_SCHEMA)) {
                stmt.setInt(1, SCHEMA_VERSION);
                try (ResultSet resultSet = stmt.executeQuery()) {
                    if (resultSet.next()) {
                        String warning = resultSet.getString(1);
                        if (warning != null) {
                            logValidationError(warning);
                        }
                        return;
                    }
                }
            }
        } catch (SQLException ex) {
            log.error("Validating schema", ex);
        }
        log.error("Incompatible database schema software=" + SCHEMA_VERSION);
        throw new HoldingsItemsException("Incompatible database schema");
    }

    void logValidationError(String warning) {
        log.warn(warning);
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
     * @throws HoldingsItemsException When database communication fails
     */
    public Set<String> getBibliographicIds(int agencyId) throws HoldingsItemsException {
        Set<String> ret = new HashSet<>();
        try (PreparedStatement stmt = connection.prepareStatement(BIBLIOGRAPHICIDS_FOR_AGENCY)) {
            stmt.setInt(1, agencyId);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    ret.add(resultSet.getString(1));
                }
            }
        } catch (SQLException ex) {
            log.error(DATABASE_ERROR, ex);
            throw new HoldingsItemsException(DATABASE_ERROR, ex);
        }
        return ret;
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
        Set<String> ret = new HashSet<>();
        try (PreparedStatement stmt = connection.prepareStatement(ISSUEIDS_FOR_RECORD)) {
            stmt.setInt(1, agencyId);
            stmt.setString(2, bibliographicId);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    ret.add(resultSet.getString(1));
                }
            }
        } catch (SQLException ex) {
            log.error(DATABASE_ERROR, ex);
            throw new HoldingsItemsException(DATABASE_ERROR, ex);
        }
        return ret;
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
    public RecordCollection getRecordCollection(String bibliographicRecordId, int agencyId, String issueId) throws HoldingsItemsException {
        try (PreparedStatement collectionStmt = connection.prepareStatement(SELECT_COLLECTION)) {
            collectionStmt.setInt(1, agencyId);
            collectionStmt.setString(2, bibliographicRecordId);
            collectionStmt.setString(3, issueId);
            RecordCollection collection = recordCollectionFromStatement(collectionStmt, bibliographicRecordId, agencyId, issueId);
            if (collection != null) {
                return collection;
            }
        } catch (SQLException ex) {
            log.error(DATABASE_ERROR, ex);
            throw new HoldingsItemsException(DATABASE_ERROR, ex);
        }
        return new RecordCollection(bibliographicRecordId, agencyId, issueId, this.getTrackingId(), this);
    }

    /**
     * Get a collection of items identified by an agencyId and an itemId
     *
     * @param agencyId  the agency
     * @param itemId    item id
     * @return an object representing a collection of records. If none are found, return null.
     * @throws HoldingsItemsException when database communication fails.
     */
    public RecordCollection getRecordCollectionItemId(int agencyId, String itemId) throws HoldingsItemsException {
        try (PreparedStatement collectionStmt = connection.prepareStatement(SELECT_HOLDING_AGENCY_ITEM)) {
            collectionStmt.setInt(2, agencyId);
            collectionStmt.setString(1, itemId);
            RecordCollection collection = recordCollectionFromItemIdStatement(collectionStmt, agencyId, itemId);
            if (collection != null) {
                return collection;
            }
        } catch (SQLException ex) {
            log.error(DATABASE_ERROR, ex);
            throw new HoldingsItemsException(DATABASE_ERROR, ex);
        }
        return null;
    }

    /**
     * Get a collection of items identified by and agencyId and a bibliographicRecordId
     *
     * @param agencyId                  id of agency
     * @param bibliographicRecordId     bibliographic record id
     * @return an object representing a collection of records. If none are found, return null.
     * @throws HoldingsItemsException when database communication fails.
     */
    public RecordCollection getRecordCollectionPid(int agencyId, String bibliographicRecordId) throws HoldingsItemsException {
        try (PreparedStatement collectionStmt = connection.prepareStatement(SELECT_COLLECTION_AGENCY_BIBLIOGRAPHICRECORDID)) {
            collectionStmt.setInt(1, agencyId);
            collectionStmt.setString(2, bibliographicRecordId);
            RecordCollection collection = recordCollectionFromPidStatement(collectionStmt, agencyId, bibliographicRecordId);
            if (collection != null) {
                return collection;
            }
        } catch (SQLException ex) {
            log.error(DATABASE_ERROR, ex);
            throw new HoldingsItemsException(DATABASE_ERROR, ex);
        }
        return null;
    }

    /**
     * Create (if required) a collection of items defined by id/library/orderId
     *
     * @param bibliographicRecordId part of the primary key
     * @param agencyId              part of the primary key
     * @param issueId               part of the primary key
     * @param modified              set modification time on the collection
     * @return record collection object
     * @throws HoldingsItemsException When database communication fails
     */
    public RecordCollection getRecordCollectionForUpdate(String bibliographicRecordId, int agencyId, String issueId, Timestamp modified) throws HoldingsItemsException {
        try {
            Savepoint savepoint = connection.setSavepoint();
            try (PreparedStatement collectionStmt = connection.prepareStatement(SELECT_COLLECTION_FOR_UPDATE)) {
                collectionStmt.setInt(1, agencyId);
                collectionStmt.setString(2, bibliographicRecordId);
                collectionStmt.setString(3, issueId);
                RecordCollection collection = recordCollectionFromStatement(collectionStmt, bibliographicRecordId, agencyId, issueId);
                if (collection != null) {
                    return collection;
                }
                collection = new RecordCollection(bibliographicRecordId, agencyId, issueId, this.getTrackingId(), this);
                collection.setCompleteTimestamp(modified);
                collection.setCreatedTimestamp(modified);
                insertCollection(collection, modified);
                connection.releaseSavepoint(savepoint);
                return collection;
            } catch (SQLException ex) {
                log.warn(DATABASE_ERROR + " Trying to premtively create/lock collection: " + ex.getMessage());
                connection.rollback(savepoint);
            }
            RecordCollection collection = getRecordCollection(bibliographicRecordId, agencyId, issueId);
            if (collection.isOriginal()) {
                log.error("Could not fetch existing, tried to create new could not fetch new.. Midt-transaction-collition weirdness");
                throw new HoldingsItemsException(DATABASE_ERROR);
            }
            return collection;
        } catch (SQLException ex) {
            log.error(DATABASE_ERROR + " Savepoint error: " + ex.getMessage());
            log.debug(DATABASE_ERROR, ex);
            throw new HoldingsItemsException(DATABASE_ERROR);
        }
    }

    /**
     * Get a set of agencies that has holdings for a record
     *
     * @param bibliographicRecordId id of record
     * @return set of agencyId
     * @throws HoldingsItemsException When database communication fails
     */
    public Set<Integer> getAgenciesThatHasHoldingsFor(String bibliographicRecordId) throws HoldingsItemsException {
        HashSet agencies = new HashSet();

        try (PreparedStatement stmt = connection.prepareStatement(AGENCIES_WITH_BIBLIOGRAPHICRECORDID)) {
            stmt.setString(1, bibliographicRecordId);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    int agency = resultSet.getInt(1);
                    agencies.add(agency);
                }
            }
        } catch (SQLException ex) {
            log.error(DATABASE_ERROR, ex);
            throw new HoldingsItemsException(DATABASE_ERROR, ex);
        }
        return agencies;
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
    public void updateBibliographicItemNote(String note, int agencyId, String bibliographicRecordId, Timestamp modified) throws HoldingsItemsException {
        try (PreparedStatement stmt = connection.prepareCall(UPDATE_COLLECTION_NOTE)) {
            int i = 1;
            stmt.setString(i++, note);
            stmt.setInt(i++, agencyId);
            stmt.setString(i++, bibliographicRecordId);
            stmt.setTimestamp(i++, modified);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            log.error(DATABASE_ERROR, ex);
            throw new HoldingsItemsException(DATABASE_ERROR, ex);
        }

    }

    /**
     * Called by RecordCollection.save()
     *
     * @param collection record collection to save
     * @param modified   modified timestamp
     * @throws HoldingsItemsException in case of a database error
     */
    void saveRecordCollection(RecordCollection collection, Timestamp modified) throws HoldingsItemsException {
        updateOrInsertCollection(collection, modified);
        try (PreparedStatement insert = connection.prepareStatement(INSERT_ITEM) ;
             PreparedStatement update = connection.prepareStatement(UPDATE_ITEM);) {
            for (Record record : collection) {
                if (record.isModified() || record.isOriginal() || collection.isOriginal()) {
                    log.debug("record = " + record);
                    if (record instanceof RecordImpl) {
                        RecordImpl item = (RecordImpl) record;
                        if (item.isOriginal() || collection.isOriginal()) {
                            Timestamp completeTimestamp = collection.getCompleteTimestamp();
                            if (completeTimestamp.after(modified)) { // Do not create new items, if there's a complete later in time.
                                log.debug("Not creating, complete later in time");
                            } else {
                                log.debug("Insert record");
                                insert.setInt(1, collection.getAgencyId());
                                insert.setString(2, collection.getBibliographicRecordId());
                                insert.setString(3, collection.getIssueId());
                                insert.setString(4, item.getItemId());
                                insert.setString(5, item.getBranch());
                                insert.setString(6, item.getDepartment());
                                insert.setString(7, item.getLocation());
                                insert.setString(8, item.getSubLocation());
                                insert.setString(9, item.getCirculationRule());
                                insert.setDate(10, new java.sql.Date(item.getAccessionDate().getTime()));
                                insert.setString(11, item.getStatus());
                                insert.setTimestamp(12, modified);
                                insert.setString(13, trackingId);
                                insert.executeUpdate();
                            }
                        } else {
                            log.debug("Update record");
                            update.setString(1, item.getBranch());
                            update.setString(2, item.getDepartment());
                            update.setString(3, item.getLocation());
                            update.setString(4, item.getSubLocation());
                            update.setString(5, item.getCirculationRule());
                            update.setDate(6, new java.sql.Date(item.getAccessionDate().getTime()));
                            update.setString(7, item.getStatus());
                            update.setTimestamp(8, modified);
                            update.setString(9, trackingId);
                            update.setInt(10, collection.getAgencyId());
                            update.setString(11, collection.getBibliographicRecordId());
                            update.setString(12, collection.getIssueId());
                            update.setString(13, item.getItemId());
                            update.setTimestamp(14, modified);
                            update.executeUpdate();
                        }
                        item.original = false;
                        item.modified = false;
                    } else {
                        throw new IllegalStateException("Cannot save records not of type RecordImpl");
                    }
                }
            }
        } catch (SQLException ex) {
            log.error(DATABASE_ERROR + ex.getMessage());
            log.info(DATABASE_ERROR, ex);
            throw new HoldingsItemsException(DATABASE_ERROR, ex);
        }
        collection.original = false;
        collection.modified = false;
        collection.complete = false;
    }

    /**
     * Create a map of status to number of items with said status
     *
     * @param bibliographicRecordId key
     * @param agencyId              key
     * @return key-value pairs with status and number of that status
     * @throws HoldingsItemsException in case of a database error
     */
    public Map<String, Integer> getStatusFor(String bibliographicRecordId, int agencyId) throws HoldingsItemsException {
        HashMap<String, Integer> map = new HashMap<>();
        try (PreparedStatement stmt = connection.prepareStatement(STATUS_OF_BIBLIOGRAPHIC_ITEMS)) {
            stmt.setInt(1, agencyId);
            stmt.setString(2, bibliographicRecordId);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    map.put(resultSet.getString(1), resultSet.getInt(2));
                }
            }
        } catch (SQLException ex) {
            log.error(DATABASE_ERROR, ex);
            throw new HoldingsItemsException(DATABASE_ERROR, ex);
        }
        return map;
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
        try (PreparedStatement stmt = connection.prepareStatement(CHECK_LIVE_HOLDINGS)) {

            stmt.setInt(1, agencyId);
            stmt.setString(2, bibliographicRecordId);
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBoolean(1);
                }
                throw new IllegalStateException("No rows from boolean query");
            }
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
     * @throws HoldingsItemsException in case of a database error
     */
    public void enqueue(String bibliographicRecordId, int agencyId, String stateChange, String worker) throws HoldingsItemsException {
        PreparedQueueSupplier supplier = getQueueSupplier();
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
        PreparedQueueSupplier supplier = getQueueSupplier();
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
        try (PreparedStatement stmt = connection.prepareStatement(QUEUE_INSERT_SQL_OLD)) {
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

    private RecordCollection recordCollectionFromStatement(final PreparedStatement collectionStmt, String bibliographicRecordId, int agencyId, String issueId) throws SQLException {
        try (ResultSet collectionResultSet = collectionStmt.executeQuery()) {
            if (collectionResultSet.next()) {
                RecordCollection collection = collectionFromResultSet(agencyId, bibliographicRecordId, issueId, collectionResultSet, this);
                try (PreparedStatement itemStmt = connection.prepareStatement(SELECT_ITEM)) {
                    itemStmt.setInt(1, agencyId);
                    itemStmt.setString(2, bibliographicRecordId);
                    itemStmt.setString(3, issueId);
                    try (ResultSet itemResultSet = itemStmt.executeQuery()) {
                        updateCollectionFromItemResultSet(collection, itemResultSet);
                    }
                }
                return collection;
            }
        }
        return null;
    }

    private RecordCollection recordCollectionFromPidStatement(final PreparedStatement collectionStatement, int agencyId, String bibliographicRecordId) throws SQLException {
        try (ResultSet collectionResultSet = collectionStatement.executeQuery()) {
            if (collectionResultSet.next()) {
                String issueId = collectionResultSet.getString(9);
                RecordCollection collection = collectionFromResultSet(agencyId, bibliographicRecordId, issueId, collectionResultSet, this);
                try (PreparedStatement itemStmt = connection.prepareStatement(SELECT_ITEM)) {
                    itemStmt.setInt(1, agencyId);
                    itemStmt.setString(2, bibliographicRecordId);
                    itemStmt.setString(3, issueId);
                    try (ResultSet itemResultSet = itemStmt.executeQuery()) {
                        updateCollectionFromItemResultSet(collection, itemResultSet);
                    }
                }
                return collection;
            }
        }
        return null;
    }

    private RecordCollection recordCollectionFromItemIdStatement(final PreparedStatement collectionStmt, int agencyId) throws SQLException {
        try (ResultSet collectionResultSet = collectionStmt.executeQuery()) {
            if (collectionResultSet.next()) {
                String issueId = collectionResultSet.getString(9);
                String bibliographicRecordId = collectionResultSet.getString(10);
                RecordCollection collection = collectionFromResultSet(agencyId, bibliographicRecordId, issueId, collectionResultSet, this);
                try (PreparedStatement itemStatement = connection.prepareStatement(SELECT_ITEM)) {
                    itemStatement.setInt(1, agencyId);
                    itemStatement.setString(2, bibliographicRecordId);
                    itemStatement.setString(3, issueId);
                    try (ResultSet itemResultSet = itemStatement.executeQuery()) {
                        updateCollectionFromItemResultSet(collection, itemResultSet);
                    }
                }
                return collection;
            }
        }
        return null;
    }

    private static void updateCollectionFromItemResultSet(RecordCollection recordCollection, ResultSet itemResultSet)  throws SQLException {
        while (itemResultSet.next()) {
            String itemId = itemResultSet.getString(1);
            String branch = itemResultSet.getString(2);
            String department = itemResultSet.getString(3);
            String location = itemResultSet.getString(4);
            String subLocation = itemResultSet.getString(5);
            String circulationRule = itemResultSet.getString(6);
            Date accessionDate = itemResultSet.getDate(7);
            String status = itemResultSet.getString(8);
            Timestamp created = itemResultSet.getTimestamp(9);
            Timestamp modified = itemResultSet.getTimestamp(10);
            String itemTrackingId = itemResultSet.getString(11);
            recordCollection.put(itemId, new RecordImpl(itemId, branch, department, location, subLocation, circulationRule, accessionDate, status, created, modified, itemTrackingId));
        }
    }

    private static RecordCollection collectionFromResultSet(int agencyId, String bibliographicRecordId, String issueId, ResultSet resultSet, HoldingsItemsDAO dao) throws SQLException {
        String issueText = resultSet.getString(1);
        Date expectedDelivery = resultSet.getTimestamp(2);
        Integer readyForLoan = resultSet.getInt(3);
        String note = resultSet.getString(4);
        Timestamp complete = resultSet.getTimestamp(5);
        Timestamp created = resultSet.getTimestamp(6);
        Timestamp modified = resultSet.getTimestamp(7);
        String colTrackingId = resultSet.getString(8);
        return new RecordCollection(bibliographicRecordId, agencyId, issueId,
                issueText, expectedDelivery, readyForLoan, note,
                complete, created, modified, colTrackingId, dao);
    }

    // CPD-OFF
    private void updateOrInsertCollection(RecordCollection collection, Timestamp modified) throws HoldingsItemsException {
        if (collection.isComplete() || collection.isOriginal()) {
            collection.setCompleteTimestamp(modified);
        }
        int affectedRows;
        try (PreparedStatement stmt = connection.prepareCall(UPDATE_COLLECTION)) {
            int i = 1;
            stmt.setString(i++, collection.getIssueText());
            Date expectedDelivery = collection.getExpectedDelivery();
            if (expectedDelivery == null) {
                stmt.setNull(i++, Types.DATE);
            } else {
                stmt.setDate(i++, new java.sql.Date(collection.getExpectedDelivery().getTime()));
            }
            stmt.setInt(i++, collection.getReadyForLoan());
            stmt.setString(i++, collection.getNote());
            stmt.setTimestamp(i++, collection.getCompleteTimestamp());
            stmt.setTimestamp(i++, modified);
            stmt.setString(i++, trackingId);
            stmt.setInt(i++, collection.getAgencyId());
            stmt.setString(i++, collection.getBibliographicRecordId());
            stmt.setString(i++, collection.getIssueId());
            stmt.setTimestamp(i++, modified);
            affectedRows = stmt.executeUpdate();
        } catch (SQLException ex) {
            log.error(DATABASE_ERROR, ex);
            throw new HoldingsItemsException(DATABASE_ERROR, ex);
        }
        if (affectedRows == 0 && collection.isOriginal()) {
            log.debug("Insert collection");
            try {
                insertCollection(collection, modified);
            } catch (SQLException ex) {
                log.error(DATABASE_ERROR, ex);
                throw new HoldingsItemsException(DATABASE_ERROR, ex);
            }
        }
    }
    // CPD-ON

    // CPD-OFF
    private void insertCollection(RecordCollection collection, Timestamp modified) throws SQLException {
        try (PreparedStatement stmt = connection.prepareCall(INSERT_COLLECTION)) {
            int i = 1;
            stmt.setInt(i++, collection.getAgencyId());
            stmt.setString(i++, collection.getBibliographicRecordId());
            stmt.setString(i++, collection.getIssueId());
            stmt.setString(i++, collection.getIssueText());
            Date expectedDelivery = collection.getExpectedDelivery();
            if (expectedDelivery == null) {
                stmt.setNull(i++, Types.DATE);
            } else {
                stmt.setDate(i++, new java.sql.Date(collection.getExpectedDelivery().getTime()));
            }
            stmt.setInt(i++, collection.getReadyForLoan());
            stmt.setString(i++, collection.getNote());
            stmt.setTimestamp(i++, collection.getCompleteTimestamp());
            stmt.setTimestamp(i++, modified);
            stmt.setString(i++, trackingId);
            stmt.executeUpdate();
        }
    }
    // CPD-ON

    private static final String DATABASE_ERROR = "Database error";

    private static final String VALIDATE_SCHEMA = "SELECT warning FROM version WHERE version=?";

    private static final String SELECT_HOLDING_AGENCY_ITEM =
                "SELECT hic.issueText, hic.expectedDelivery, hic.readyForLoan, hic.note, hic.complete, hic.created, hic.modified, hic.trackingId, hic.issueId, hic.bibliographicRecordId" +
                " FROM holdingsitemscollection hic, holdingsitemsitem hii WHERE" +
                " hii.agencyId=hic.agencyId AND hii.bibliographicRecordId=hic.bibliographicRecordId AND hii.issueId=hic.issueId AND" +
                " hii.itemId=? AND hic.agencyId=?";

    private static final String SELECT_COLLECTION_AGENCY_BIBLIOGRAPHICRECORDID =
            "SELECT issueText, expectedDelivery, readyForLoan, note, complete, created, modified, trackingId, issueId" +
            " FROM holdingsitemscollection WHERE agencyId=? AND bibliographicRecordId=?";

    private static final String SELECT_COLLECTION = "SELECT issueText, expectedDelivery, readyForLoan, note, complete, created, modified, trackingId" +
            " FROM holdingsitemscollection WHERE agencyId=? AND bibliographicRecordId=? AND issueId=?";

    private static final String SELECT_COLLECTION_FOR_UPDATE = SELECT_COLLECTION + " FOR UPDATE";
    private static final String UPDATE_COLLECTION = "UPDATE holdingsitemscollection" +
                                                    " SET issueText=?, expectedDelivery=?, readyForLoan=?, note=?, complete=?, modified=?, trackingId=?" +
                                                    " WHERE agencyId=? AND bibliographicRecordId=? AND issueId=? AND modified<=?";
    private static final String UPDATE_COLLECTION_NOTE = "UPDATE holdingsitemscollection" +
                                                         " SET note=?" +
                                                         " WHERE agencyId=? AND bibliographicRecordId=? AND modified<=?";
    private static final String INSERT_COLLECTION = "INSERT INTO holdingsitemscollection" +
                                                    " (agencyId, bibliographicRecordId, issueId, issueText, expectedDelivery, readyForLoan, note, complete, created, modified, trackingId)" +
                                                    " VALUES (?, ?, ?, ?, ?, ?, ?, ?, timeofday()::timestamp, ?, ?)";
    private static final String SELECT_ITEM = "SELECT itemId, branch, department, location, subLocation, circulationRule, accessionDate, status, created, modified, trackingId" +
                                              " FROM holdingsitemsitem WHERE agencyId=? AND bibliographicRecordId=? AND issueId=?";
    private static final String UPDATE_ITEM = "UPDATE holdingsitemsitem" +
                                              " SET branch=?, department=?, location=?, subLocation=?, circulationRule=?, accessionDate=?, status=?," +
                                              " modified=?, trackingId=?" +
                                              " WHERE agencyId=? AND bibliographicRecordId=? AND issueId=? AND itemId=? AND modified<=?";
    private static final String INSERT_ITEM = "INSERT INTO holdingsitemsitem" +
                                              " (agencyId, bibliographicRecordId, issueId, itemId," +
                                              " branch, department, location, subLocation, circulationRule, accessionDate, status," +
                                              " created, modified, trackingId)" +
                                              " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, timeofday()::timestamp, ?, ?)";

    private static final String BIBLIOGRAPHICIDS_FOR_AGENCY = "SELECT DISTINCT bibliographicRecordId FROM holdingsitemscollection" +
                                                              " WHERE agencyId=?";
    private static final String ISSUEIDS_FOR_RECORD = "SELECT DISTINCT issueId FROM holdingsitemscollection" +
                                                      " WHERE agencyId=? AND bibliographicRecordId=?";
    private static final String AGENCIES_WITH_BIBLIOGRAPHICRECORDID = "SELECT DISTINCT agencyId FROM holdingsitemscollection" +
                                                                      " JOIN holdingsitemsitem USING(agencyId, bibliographicRecordId, issueId)" +
                                                                      " WHERE bibliographicRecordId=? AND status != 'Decommissioned'";
    private static final String STATUS_OF_BIBLIOGRAPHIC_ITEMS = "SELECT status, COUNT(*) FROM holdingsitemscollection" +
                                                                " JOIN holdingsitemsitem USING(agencyid, bibliographicrecordid, issueid)" +
                                                                " WHERE agencyid=? AND bibliographicrecordid=?" +
                                                                " GROUP BY status;";

    private static final String CHECK_LIVE_HOLDINGS = "SELECT EXISTS(SELECT * FROM holdingsitemsitem WHERE agencyid=? AND bibliographicrecordid=? AND status<>'Decommissioned')";

    private static final String QUEUE_INSERT_SQL_OLD = "INSERT INTO q(worker, queued, bibliographicRecordId, agencyId, additionalData, trackingId) VALUES(?, clock_timestamp() + ? * INTERVAL '1 milliseconds', ?, ?, ?, ?)";

}
