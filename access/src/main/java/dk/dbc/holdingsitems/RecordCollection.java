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

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class RecordCollection implements Iterable<Record> {

    private final String bibliographicRecordId;
    private final int AgencyId;
    private final String issueId;
    private String issueText;
    private Date expectedDelivery;
    private int readyForLoan;
    private String note;
    private Timestamp completeTimestamp;
    private Timestamp createdTimestamp;
    private Timestamp modifiedTimestamp;

    final private HashMap<String, Record> records;
    final private HoldingsItemsDAO dao;

    boolean complete;
    boolean modified;
    boolean original;
    private final String trackingId;

    public RecordCollection(String bibliographicRecordId, int agencyId, String issueId, String issueText,
                            Date expectedDelivery, int readyForLoan, String note,
                            Timestamp completeTimestamp, Timestamp createdTimestamp, Timestamp modifiedTimestamp,
                            String trackingId, HoldingsItemsDAO dao) {
        this.bibliographicRecordId = bibliographicRecordId;
        this.AgencyId = agencyId;
        this.issueId = issueId;
        this.issueText = issueText;
        this.expectedDelivery = expectedDelivery == null ? null : (Date) expectedDelivery.clone();
        this.readyForLoan = readyForLoan;
        this.note = note;
        this.completeTimestamp = (Timestamp) completeTimestamp.clone();
        this.createdTimestamp = (Timestamp) createdTimestamp.clone();
        this.modifiedTimestamp = (Timestamp) modifiedTimestamp.clone();

        this.records = new HashMap<>();

        this.dao = dao;
        this.complete = false;
        this.modified = false;
        this.original = false;
        this.trackingId = trackingId;
    }

    public RecordCollection(String bibliographicRecordId, int agencyId, String issueId, String trackingId, HoldingsItemsDAO dao) {
        this.bibliographicRecordId = bibliographicRecordId;
        this.AgencyId = agencyId;
        this.issueId = issueId;
        this.issueText = "";
        this.expectedDelivery = null;
        this.readyForLoan = -1;
        this.note = "";
        this.createdTimestamp = new Timestamp(new Date().getTime());
        this.modifiedTimestamp = createdTimestamp;

        this.records = new HashMap<>();

        this.dao = dao;
        this.complete = true;
        this.modified = true;
        this.original = true;
        this.trackingId = trackingId;
    }

    public String getBibliographicRecordId() {
        return bibliographicRecordId;
    }

    public int getAgencyId() {
        return AgencyId;
    }

    public String getIssueId() {
        return issueId;
    }

    public String getIssueText() {
        return issueText;
    }

    public void setIssueText(String issueText) {
        if (!this.issueText.equals(issueText)) {
            this.modified = true;
            this.issueText = issueText;
        }
    }

    public String getTrackingId() {
        return trackingId;
    }

    public Date getExpectedDelivery() {
        if (expectedDelivery == null) {
            return null;
        } else {
            return (Date) expectedDelivery.clone();
        }
    }

    public void setExpectedDelivery(Date expectedDelivery) {
        this.modified = true;
        if (expectedDelivery == null) {
            if (this.expectedDelivery != null) {
                this.modified = true;
                this.expectedDelivery = null;
            }
        } else {
            if (!expectedDelivery.equals(this.expectedDelivery)) {
                this.modified = true;
                this.expectedDelivery = (Date) expectedDelivery.clone();
            }
        }
    }

    public int getReadyForLoan() {
        return readyForLoan;
    }

    public void setReadyForLoan(int readyForLoan) {
        if (this.readyForLoan != readyForLoan) {
            this.modified = true;
            this.readyForLoan = readyForLoan;
        }
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(String note) {
        if (!this.note.equals(note)) {
            this.modified = true;
            this.note = note;
        }
    }

    public Timestamp getCompleteTimestamp() {
        return (Timestamp) completeTimestamp.clone();
    }

    public void setCompleteTimestamp(Timestamp completeTimestamp) {
        this.completeTimestamp = (Timestamp) completeTimestamp.clone();
    }

    public Timestamp getCreatedTimestamp() {
        return (Timestamp) createdTimestamp.clone();
    }

    public void setCreatedTimestamp(Timestamp createdTimestamp) {
        if (!this.createdTimestamp.equals(createdTimestamp)) {
            this.modified = true;
            this.createdTimestamp = (Timestamp) createdTimestamp.clone();
        }
    }

    public Timestamp getModifiedTimestamp() {
        return (Timestamp) modifiedTimestamp.clone();
    }

    public void setModifiedTimestamp(Timestamp modifiedTimestamp) {
        if (!this.modifiedTimestamp.equals(modifiedTimestamp)) {
            this.modified = true;
            this.modifiedTimestamp = (Timestamp) modifiedTimestamp.clone();
        }
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public boolean isModified() {
        return modified;
    }

    public boolean isOriginal() {
        return original;
    }

    /**
     * Locate or create record in collection
     *
     * @param itemId find item in collection
     * @return record from set (or newly created record)
     */
    public Record findRecord(String itemId) {
        if (records.containsKey(itemId)) {
            Record record = records.get(itemId);
            if (complete && record instanceof RecordImpl) {
                ( (RecordImpl) record ).modified = true;
            }
            return record;
        }
        Record record = new RecordImpl(itemId);
        records.put(itemId, record);
        return record;
    }

    /**
     * Save all the records
     *
     * @param modified When the modified timestamp should be set to
     * @throws HoldingsItemsException If database communication failed
     */
    public void save(Timestamp modified) throws HoldingsItemsException {
        dao.saveRecordCollection(this, modified);
    }

    /**
     * Save all the records
     *
     * @throws HoldingsItemsException in case of database errors
     */
    public void save() throws HoldingsItemsException {
        save(new Timestamp(new Date().getTime()));
    }

    @Override
    public Iterator<Record> iterator() {
        return records.values().iterator();
    }

    public void put(String itemId, Record record) {
        records.put(itemId, record);
    }

    @Override
    public String toString() {
        return "RecordCollection{" + "bibliographicRecordId=" + bibliographicRecordId + ", AgencyId=" + AgencyId + ", issueId=" + issueId + ", records.size()=" + records.size() + "}";
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(this.bibliographicRecordId);
        hash = 47 * hash + this.AgencyId;
        hash = 47 * hash + Objects.hashCode(this.issueId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RecordCollection other = (RecordCollection) obj;
        if (!Objects.equals(this.bibliographicRecordId, other.bibliographicRecordId)) {
            return false;
        }
        if (this.AgencyId != other.AgencyId) {
            return false;
        }
        if (!Objects.equals(this.issueId, other.issueId)) {
            return false;
        }
        return true;
    }

}
