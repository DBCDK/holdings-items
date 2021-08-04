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

import dk.dbc.pgqueue.common.DeduplicateAbstraction;
import dk.dbc.pgqueue.common.QueueStorageAbstraction;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class QueueJob {

    private String bibliographicRecordId;
    private int agencyId;
    private String stateChange;
    private String trackingId;
    private Set<String> trackingIds;

    QueueJob() {
        this.trackingIds = null;
    }

    public QueueJob(int agencyId, String bibliographicRecordId, String stateChange, String trackingId) {
        this.trackingIds = null;
        this.agencyId = agencyId;
        this.bibliographicRecordId = bibliographicRecordId;
        this.stateChange = stateChange;
        this.trackingId = trackingId;
    }

    public String getBibliographicRecordId() {
        return bibliographicRecordId;
    }

    public int getAgencyId() {
        return agencyId;
    }

    public String getStateChange() {
        return stateChange;
    }

    public String getTrackingId() {
        return String.join("\t", getTrackingIds());
    }

    private Set<String> getTrackingIds() {
        synchronized (this) {
            if (trackingIds == null) {
                if (trackingId == null || trackingId.isEmpty()) {
                    trackingIds = new TreeSet<>();
                } else {
                    trackingIds = new TreeSet<>(Arrays.asList(this.trackingId.split("\t")));
                }
            }
            return trackingIds;
        }
    }

    private void addTrackingIds(Collection<String> trackingIds) {
        synchronized (this) {
            getTrackingIds().addAll(trackingIds);
        }
    }

    @Override
    public String toString() {
        return "QueueJob{" + "bibliographicRecordId=" + bibliographicRecordId + ", agencyId=" + agencyId + ", stateChange=" + stateChange + ", trackingId=" + getTrackingId() + '}';
    }

    public static final QueueStorageAbstraction<QueueJob> STORAGE_ABSTRACTION = new QueueStorageAbstraction<QueueJob>() {
        private final String[] COLUMNS = "agencyId,bibliographicRecordId,stateChange,trackingId".split(",");

        @Override
        public String[] columnList() {
            return COLUMNS;
        }

        @Override
        public QueueJob createJob(ResultSet resultSet, int startColumn) throws SQLException {
            int agencyId = resultSet.getInt(startColumn++);
            String bibliographicRecordId = resultSet.getString(startColumn++);
            String stateChange = resultSet.getString(startColumn++);
            String trackingId = resultSet.getString(startColumn++);
            return new QueueJob(agencyId, bibliographicRecordId, stateChange, trackingId);
        }

        @Override
        public void saveJob(QueueJob job, PreparedStatement stmt, int startColumn) throws SQLException {
            stmt.setInt(startColumn++, job.getAgencyId());
            stmt.setString(startColumn++, job.getBibliographicRecordId());
            stmt.setString(startColumn++, job.getStateChange());
            stmt.setString(startColumn++, job.getTrackingId());
        }

    };
    public static final DeduplicateAbstraction<QueueJob> DEDUPLICATION_ABSTRACTION_IGNORE_STATECHANGE = new DeduplicateAbstraction<QueueJob>() {
        private final String[] COLUMNS = "agencyId,bibliographicRecordId".split(",");

        @Override
        public String[] duplicateDeleteColumnList() {
            return COLUMNS;
        }

        @Override
        public void duplicateValues(QueueJob job, PreparedStatement stmt, int startColumn) throws SQLException {
            stmt.setInt(startColumn++, job.getAgencyId());
            stmt.setString(startColumn++, job.getBibliographicRecordId());
        }

        @Override
        public QueueJob mergeJob(QueueJob originalJob, QueueJob skippedJob) {
            originalJob.addTrackingIds(skippedJob.getTrackingIds());
            return originalJob;
        }
    };
}
