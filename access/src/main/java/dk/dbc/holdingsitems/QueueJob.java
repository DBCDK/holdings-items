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

import dk.dbc.pgqueue.QueueStorageAbstraction;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class QueueJob {

    public static final String SQL_COLUMNS = "worker, queued, bibliographicRecordId, agencyId, additionalData, trackingId";

    protected String worker;
    protected Instant queued;
    protected String bibliographicRecordId;
    protected int agencyId;
    protected String additionalData;
    protected String trackingId;

    public QueueJob(ResultSet resultSet) throws SQLException {
        this(resultSet, 1);
    }

    public QueueJob(ResultSet resultSet, int column) throws SQLException {
        this.worker = resultSet.getString(column++);
        this.queued = resultSet.getTimestamp(column++).toInstant();
        this.bibliographicRecordId = resultSet.getString(column++);
        this.agencyId = resultSet.getInt(column++);
        this.additionalData = resultSet.getString(column++);
        this.trackingId = resultSet.getString(column++);
    }

    QueueJob() {
    }

    public QueueJob(String bibliographicRecordId, int agencyId, String additionalData) {
        this.bibliographicRecordId = bibliographicRecordId;
        this.agencyId = agencyId;
        this.additionalData = additionalData;
    }

    public QueueJob(int agencyId, String bibliographicRecordId, String trackingId) {
        this.agencyId = agencyId;
        this.bibliographicRecordId = bibliographicRecordId;
        this.additionalData = additionalData;
        this.trackingId = trackingId;
    }

    public QueueJob(String worker, Instant queued, String bibliographicRecordId, int agencyId, String additionalData, String trackingId) {
        this.worker = worker;
        this.queued = queued;
        this.bibliographicRecordId = bibliographicRecordId;
        this.agencyId = agencyId;
        this.additionalData = additionalData;
        this.trackingId = trackingId;
    }

    public String getWorker() {
        return worker;
    }

    public Instant getQueued() {
        return queued;
    }

    public String getBibliographicRecordId() {
        return bibliographicRecordId;
    }

    public int getAgencyId() {
        return agencyId;
    }

    public String getAdditionalData() {
        return additionalData;
    }

    public String getTrackingId() {
        return trackingId;
    }

    @Override
    public String toString() {
        return "QueueJob{" + "worker=" + worker + ", queued=" + queued + ", bibliographicRecordId=" + bibliographicRecordId + ", agencyId=" + agencyId + ", additionalData=" + additionalData + ", trackingId=" + trackingId + '}';
    }

    public static final QueueStorageAbstraction<QueueJob> STORAGE_ABSTRACTION = new QueueStorageAbstraction<QueueJob>() {
        private final String[] COLUMNS = "agencyId,bibliographicRecordId,trackingId".split(",");

        @Override
        public String[] columnList() {
            return COLUMNS;
        }

        @Override
        public QueueJob createJob(ResultSet resultSet, int startColumn) throws SQLException {
            int agencyId = resultSet.getInt(startColumn++);
            String bibliographicRecordId = resultSet.getString(startColumn++);
            String trackingId = resultSet.getString(startColumn++);
            return new QueueJob(agencyId, bibliographicRecordId, trackingId);
        }

        @Override
        public void saveJob(QueueJob job, PreparedStatement stmt, int startColumn) throws SQLException {
            stmt.setInt(startColumn++, job.getAgencyId());
            stmt.setString(startColumn++, job.getBibliographicRecordId());
            stmt.setString(startColumn++, job.getTrackingId());
        }

    };
}
