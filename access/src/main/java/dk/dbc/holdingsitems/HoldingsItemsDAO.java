/*
 * dbc-holdings-items-access
 * Copyright (C) 2014 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of dbc-holdings-items-access.
 *
 * dbc-holdings-items-access is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dbc-holdings-items-access is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with dbc-holdings-items-access.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class HoldingsItemsDAO {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HoldingsItemsDAO.class);

    private static final int SCHEMA_VERSION = 14;

    private final Connection connection;
    private final String trackingId;

    /**
     * Constructor
     *
     * @param connection Database connection
     * @param trackingId
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
     * @throws HoldingsItemsException
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
     * @return
     */
    public String getTrackingId() {
        return trackingId;
    }

    /**
     *
     * @param agencyId
     * @return
     * @throws HoldingsItemsException
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
     *
     * @param bibliographicId
     * @param agencyId
     * @return
     * @throws HoldingsItemsException
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
     * @param bibliographicRecordId
     * @param agencyId
     * @param issueId
     * @return
     * @throws HoldingsItemsException
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
     * Create (if required) a collection of items defined by id/library/orderId
     *
     * @param bibliographicRecordId
     * @param agencyId
     * @param issueId
     * @param modified
     * @return
     * @throws HoldingsItemsException
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
     * @throws HoldingsItemsException
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
     * Called by RecordCollection.save()
     *
     * @param collection
     * @throws HoldingsItemsException
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
     * @param bibliographicRecordId
     * @param agencyId
     * @return
     * @throws HoldingsItemsException
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
     * @param bibliographicRecordId
     * @param agencyId
     * @return
     * @throws HoldingsItemsException
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
     * @param worker                Who's get the job
     * @throws HoldingsItemsException
     */
    public void enqueue(String bibliographicRecordId, int agencyId, String worker) throws HoldingsItemsException {
        enqueue(bibliographicRecordId, agencyId, worker, 0);
    }

    /**
     * Put an element into the queue
     *
     * @param bibliographicRecordId part of job id
     * @param agencyId              part of job id
     * @param worker                Who's get the job
     * @param milliSeconds          How many milliseconds from now it should be
     *                              dequeued at the earliest
     * @throws HoldingsItemsException
     */
    public void enqueue(String bibliographicRecordId, int agencyId, String worker, long milliSeconds) throws HoldingsItemsException {
        enqueue(bibliographicRecordId, agencyId, "", worker, milliSeconds);
    }

    /**
     * Put an element into the queue
     *
     * @param bibliographicRecordId part of job id
     * @param agencyId              part of job id
     * @param additionalData        extra data for job
     * @param worker                Who's to get the job
     * @throws HoldingsItemsException
     */
    public void enqueue(String bibliographicRecordId, int agencyId, String additionalData, String worker) throws HoldingsItemsException {
        enqueue(bibliographicRecordId, agencyId, worker, 0);
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
     * @throws HoldingsItemsException
     */
    public void enqueue(String bibliographicRecordId, int agencyId, String additionalData, String worker, long milliSeconds) throws HoldingsItemsException {
        try (PreparedStatement stmt = connection.prepareStatement(QUEUE_INSERT_SQL)) {
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
        try (final ResultSet collectionResultSet = collectionStmt.executeQuery()) {
            if (collectionResultSet.next()) {
                String issueText = collectionResultSet.getString(1);
                Date expectedDelivery = collectionResultSet.getTimestamp(2);
                Integer readyForLoan = collectionResultSet.getInt(3);
                String note = collectionResultSet.getString(4);
                Timestamp complete = collectionResultSet.getTimestamp(5);
                Timestamp created = collectionResultSet.getTimestamp(6);
                Timestamp modified = collectionResultSet.getTimestamp(7);
                String colTrackingId = collectionResultSet.getString(8);
                RecordCollection collection = new RecordCollection(bibliographicRecordId, agencyId, issueId,
                                                                   issueText, expectedDelivery, readyForLoan, note,
                                                                   complete, created, modified, colTrackingId, this);
                try (PreparedStatement itemStmt = connection.prepareStatement(SELECT_ITEM)) {
                    itemStmt.setInt(1, agencyId);
                    itemStmt.setString(2, bibliographicRecordId);
                    itemStmt.setString(3, issueId);
                    try (ResultSet itemResultSet = itemStmt.executeQuery()) {
                        while (itemResultSet.next()) {
                            String itemId = itemResultSet.getString(1);
                            String branch = itemResultSet.getString(2);
                            String department = itemResultSet.getString(3);
                            String location = itemResultSet.getString(4);
                            String subLocation = itemResultSet.getString(5);
                            String circulationRule = itemResultSet.getString(6);
                            Date accessionDate = itemResultSet.getDate(7);
                            String status = itemResultSet.getString(8);
                            created = itemResultSet.getTimestamp(9);
                            modified = itemResultSet.getTimestamp(10);
                            String itemTrackingId = itemResultSet.getString(11);
                            collection.put(itemId, new RecordImpl(itemId, branch, department, location, subLocation, circulationRule, accessionDate, status, created, modified, itemTrackingId));
                        }
                    }
                }
                return collection;
            }
        }
        return null;
    }

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
        if (collection.isComplete()) {
            log.debug("collection.isComplete()");
            try (PreparedStatement stmt = connection.prepareCall(DECOMMISSION_ITEMS)) {
                stmt.setTimestamp(1, modified);
                stmt.setString(2, trackingId);
                stmt.setInt(3, collection.getAgencyId());
                stmt.setString(4, collection.getBibliographicRecordId());
                stmt.setString(5, collection.getIssueId());
                stmt.setTimestamp(6, modified);
                stmt.executeUpdate();
            } catch (SQLException ex) {
                log.error(DATABASE_ERROR, ex);
                throw new HoldingsItemsException(DATABASE_ERROR, ex);
            }
        }
    }

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

    private static final String DATABASE_ERROR = "Database error";

    private static final String VALIDATE_SCHEMA = "SELECT warning FROM version WHERE version=?";

    private static final String SELECT_COLLECTION = "SELECT issueText, expectedDelivery, readyForLoan, note, complete, created, modified, trackingId" +
                                                    " FROM holdingsitemscollection WHERE agencyId=? AND bibliographicRecordId=? AND issueId=?";
    private static final String SELECT_COLLECTION_FOR_UPDATE = SELECT_COLLECTION + " FOR UPDATE";
    private static final String UPDATE_COLLECTION = "UPDATE holdingsitemscollection" +
                                                    " SET issueText=?, expectedDelivery=?, readyForLoan=?, note=?, complete=?, modified=?, trackingId=?" +
                                                    " WHERE agencyId=? AND bibliographicRecordId=? AND issueId=? AND modified<=?";
    private static final String INSERT_COLLECTION = "INSERT INTO holdingsitemscollection" +
                                                    " (agencyId, bibliographicRecordId, issueId, issueText, expectedDelivery, readyForLoan, note, complete, created, modified, trackingId)" +
                                                    " VALUES (?, ?, ?, ?, ?, ?, ?, ?, timeofday()::timestamp, ?, ?)";
    private static final String SELECT_ITEM = "SELECT itemId, branch, department, location, subLocation, circulationRule, accessionDate, status, created, modified, trackingId" +
                                              " FROM holdingsitemsitem WHERE agencyId=? AND bibliographicRecordId=? AND issueId=?";
    private static final String UPDATE_ITEM = "UPDATE holdingsitemsitem" +
                                              " SET branch=?, department=?, location=?, subLocation=?, circulationRule=?, accessionDate=?, status=?," +
                                              " modified=?, trackingId=?" +
                                              " WHERE agencyId=? AND bibliographicRecordId=? AND issueId=? AND itemId=? AND modified<=?";
    private static final String DECOMMISSION_ITEMS = "UPDATE holdingsitemsitem" +
                                                     " SET status='Decommissioned', modified=?, trackingId=?" +
                                                     " WHERE agencyId=? AND bibliographicRecordId=? AND issueId=? AND modified<=? AND status<>'Online'";
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

    private static final String QUEUE_INSERT_SQL = "INSERT INTO q(worker, queued, bibliographicRecordId, agencyId, additionalData, trackingId) VALUES(?, clock_timestamp() + ? * INTERVAL '1 milliseconds', ?, ?, ?, ?)";

}
