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
import java.util.Objects;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class RecordImpl implements Record {

    String itemId;
    String branch;
    String department;
    String location;
    String subLocation;
    String circulationRule;
    String status;
    Date accessionDate;

    Timestamp createdTimestamp;
    Timestamp modifiedTimestamp;

    boolean modified;
    boolean original;
    private final String trackingId;

    RecordImpl(String itemId) {
        this.itemId = itemId;
        this.branch = "";
        this.department = "";
        this.location = "";
        this.subLocation = "";
        this.circulationRule = "";
        this.accessionDate = null;
        this.status = "";
        this.createdTimestamp = new Timestamp(new Date().getTime());
        this.modifiedTimestamp = this.createdTimestamp;
        this.modified = true;
        this.original = true;
        this.trackingId = "";
    }

    public RecordImpl(String itemId, String branch, String department, String location, String subLocation, String circulationRule, Date accessionDate, String status, Timestamp created, Timestamp modified, String trackingId) {
        this.itemId = itemId;
        this.branch = branch;
        this.department = department;
        this.location = location;
        this.subLocation = subLocation;
        this.circulationRule = circulationRule;
        this.accessionDate = (Date) accessionDate.clone();
        this.status = status;
        this.createdTimestamp = (Timestamp) created.clone();
        this.modifiedTimestamp = (Timestamp) modified.clone();
        this.modified = false;
        this.original = false;
        this.trackingId = trackingId;
    }

    @Override
    public String getItemId() {
        return itemId;
    }

    @Override
    public String getBranch() {
        return branch;
    }

    @Override
    public void setBranch(String branch) {
        if (!this.branch.equals(branch)) {
            this.modified = true;
            this.branch = branch;
        }
    }

    @Override
    public String getDepartment() {
        return department;
    }

    @Override
    public void setDepartment(String department) {
        if (!this.department.equalsIgnoreCase(department)) {
            this.modified = true;
            this.department = department;
        }
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public void setLocation(String location) {
        if (!this.location.equals(location)) {
            this.modified = true;
            this.location = location;
        }
    }

    @Override
    public String getSubLocation() {
        return subLocation;
    }

    @Override
    public void setSubLocation(String subLocation) {
        if (!this.subLocation.equals(subLocation)) {
            this.modified = true;
            this.subLocation = subLocation;
        }
    }

    @Override
    public String getCirculationRule() {
        return circulationRule;
    }

    @Override
    public void setCirculationRule(String circulationRule) {
        if (!this.circulationRule.equals(circulationRule)) {
            this.modified = true;
            this.circulationRule = circulationRule;
        }
    }

    @Override
    public Date getAccessionDate() {
        return (Date) ( accessionDate == null ? null : accessionDate.clone() );
    }

    @Override
    public void setAccessionDate(Date accessionDate) {
        if (this.accessionDate == null && accessionDate != null
            || this.accessionDate != null && !this.accessionDate.equals(accessionDate)) {
            this.modified = true;
            this.accessionDate = (Date) accessionDate.clone();
        }
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(String status) {
        switch (status) {
            case Decommissioned:
            case NotForLoan:
            case OnLoan:
            case OnOrder:
            case OnShelf:
            case Online:
                if (!this.status.equals(status)) {
                    this.modified = true;
                    this.status = status;
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown status: " + status);
        }
    }

    public Timestamp getCreatedTimestamp() {
        return (Timestamp) createdTimestamp.clone();
    }

    public Timestamp getModifiedTimestamp() {
        return (Timestamp) modifiedTimestamp.clone();
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public boolean isOriginal() {
        return original;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.itemId);
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
        final RecordImpl other = (RecordImpl) obj;
        if (!Objects.equals(this.itemId, other.itemId)) {
            return false;
        }
        return true;
    }

    @Override
    public Timestamp getCreated() {
        return (Timestamp) createdTimestamp.clone();
    }

    @Override
    public Timestamp getModified() {
        return (Timestamp) modifiedTimestamp.clone();
    }

    @Override
    public String getTrackingId() {
        return trackingId;
    }

    @Override
    public String toString() {
        return "Record{" + "itemId=" + itemId + '}';
    }

}
