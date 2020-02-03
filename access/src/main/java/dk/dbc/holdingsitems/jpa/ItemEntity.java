/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items-access
 *
 * holdings-items-access is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items-access is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.jpa;

import java.io.Serializable;
import java.sql.Timestamp;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Entity
@Table(name = "item")
@SuppressWarnings("PMD.UnusedPrivateField")
public class ItemEntity implements Serializable {

    private static final long serialVersionUID = 1089023457634768914L;

    @EmbeddedId
    private final ItemKey pk;

    // Mirrors of values from ItemKey
    // Needed for EntityManager.createQuery to access these fields
    // Primary Key
    @Column(updatable = false, nullable = false)
    private int agencyId;

    @Column(updatable = false, nullable = false)
    private String bibliographicRecordId;

    @Column(updatable = false, nullable = false)
    private String issueId;

    @Column(updatable = false, nullable = false)
    private String itemId;

    // Data fields
    @Column(nullable = false)
    private String branch;

    @Column(nullable = false)
    private String branchId;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String subLocation;

    @Column(nullable = false)
    private String circulationRule;

    @Column(nullable = false)
    private Date accessionDate;

    @Column(nullable = false)
    private String loanRestriction;

    @Column(nullable = false)
    @Convert(converter = StatusConverter.class)
    private Status status;

    @Column(nullable = false)
    private Timestamp modified;

    @Column(nullable = false)
    private Timestamp created;

    @Column(nullable = false)
    private String trackingId;

    @MapsId("collection") // Refers to pk(ItemKey).collection
    // Columns needs to be insertable=false, updatable=false to not collide with mirrored fields
    @JoinColumns({
        @JoinColumn(name = "agencyId", referencedColumnName = "agencyId",
                    insertable = false, updatable = false),
        @JoinColumn(name = "bibliographicRecordId", referencedColumnName = "bibliographicRecordId",
                    insertable = false, updatable = false),
        @JoinColumn(name = "issueId", referencedColumnName = "issueId",
                    insertable = false, updatable = false)
    })

    @ManyToOne(cascade = CascadeType.ALL)
    IssueEntity owner;

    @Transient
    transient boolean persist;

    public ItemEntity() {
        this.pk = new ItemKey();
        this.persist = false;
    }

    ItemEntity(IssueEntity owner, String itemId) {
        this.pk = new ItemKey();
        this.agencyId = owner.getAgencyId();
        this.bibliographicRecordId = owner.getBibliographicRecordId();
        this.issueId = owner.getIssueId();
        this.itemId = itemId;
        this.status = Status.UNKNOWN;
        this.loanRestriction = "";
        this.owner = owner;
        this.persist = true;
    }

    public void remove() {
        owner.removeItem(this);
    }

    void persisted() {
        persist = false;
    }

    public boolean isNew() {
        return this.persist;
    }

    // Accessors (immutable)
    public Integer getAgencyId() {
        return agencyId;
    }

    public String getBibliographicRecordId() {
        return bibliographicRecordId;
    }

    public String getIssueId() {
        return issueId;
    }

    public String getItemId() {
        return itemId;
    }

    // Accessors
    public String getBranch() {
        return branch;
    }

    public ItemEntity setBranch(String branch) {
        this.branch = branch;
        return this;
    }

    public String getBranchId() {
        return branchId;
    }

    public ItemEntity setBranchId(String branchId) {
        this.branchId = branchId;
        return this;
    }

    public String getDepartment() {
        return department;
    }

    public ItemEntity setDepartment(String department) {
        this.department = department;
        return this;
    }

    public String getLoanRestriction() {
        return loanRestriction;
    }

    public ItemEntity setLoanRestriction(String loanRestriction) {
        this.loanRestriction = loanRestriction;
        return this;
    }

    public String getLocation() {
        return location;
    }

    public ItemEntity setLocation(String location) {
        this.location = location;
        return this;
    }

    public String getSubLocation() {
        return subLocation;
    }

    public ItemEntity setSubLocation(String subLocation) {
        this.subLocation = subLocation;
        return this;
    }

    public String getCirculationRule() {
        return circulationRule;
    }

    public ItemEntity setCirculationRule(String circulationRule) {
        this.circulationRule = circulationRule;
        return this;
    }

    public LocalDate getAccessionDate() {
        return accessionDate.toLocalDate();
    }

    /**
     * Set accessionDate (when something was received)
     * <p>
     * Also propagate it up the structure eventually to
     * {@link BibliographicItemEntity#setFirstAccessionDate(java.time.LocalDate)}
     *
     * @param accessionDate date of accession
     * @return self
     */
    public ItemEntity setAccessionDate(LocalDate accessionDate) {
        this.accessionDate = Date.valueOf(accessionDate);
        owner.setFirstAccessionDate(accessionDate);
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public ItemEntity setStatus(Status status) {
        this.status = status;
        return this;
    }

    public Instant getModified() {
        return modified.toInstant();
    }

    public ItemEntity setModified(Instant modified) {
        this.modified = Timestamp.from(modified);
        return this;
    }

    public Instant getCreated() {
        return created.toInstant();
    }

    public ItemEntity setCreated(Instant created) {
        this.created = Timestamp.from(created);
        return this;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public ItemEntity setTrackingId(String trackingId) {
        this.trackingId = trackingId;
        return this;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + this.agencyId;
        hash = 67 * hash + Objects.hashCode(this.bibliographicRecordId);
        hash = 67 * hash + Objects.hashCode(this.issueId);
        hash = 67 * hash + Objects.hashCode(this.itemId);
        hash = 67 * hash + Objects.hashCode(this.branch);
        hash = 67 * hash + Objects.hashCode(this.branchId);
        hash = 67 * hash + Objects.hashCode(this.department);
        hash = 67 * hash + Objects.hashCode(this.location);
        hash = 67 * hash + Objects.hashCode(this.subLocation);
        hash = 67 * hash + Objects.hashCode(this.circulationRule);
        hash = 67 * hash + Objects.hashCode(this.accessionDate);
        hash = 67 * hash + Objects.hashCode(this.loanRestriction);
        hash = 67 * hash + Objects.hashCode(this.status);
        hash = 67 * hash + Objects.hashCode(this.modified);
        hash = 67 * hash + Objects.hashCode(this.created);
        hash = 67 * hash + Objects.hashCode(this.trackingId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final ItemEntity other = (ItemEntity) obj;
        return this.agencyId == other.agencyId &&
               Objects.equals(this.bibliographicRecordId, other.bibliographicRecordId) &&
               Objects.equals(this.issueId, other.issueId) &&
               Objects.equals(this.itemId, other.itemId) &&
               Objects.equals(this.branch, other.branch) &&
               Objects.equals(this.branchId, other.branchId) &&
               Objects.equals(this.department, other.department) &&
               Objects.equals(this.location, other.location) &&
               Objects.equals(this.subLocation, other.subLocation) &&
               Objects.equals(this.circulationRule, other.circulationRule) &&
               Objects.equals(this.accessionDate, other.accessionDate) &&
               Objects.equals(this.loanRestriction, other.loanRestriction) &&
               Objects.equals(this.status, other.status) &&
               Objects.equals(this.modified, other.modified) &&
               Objects.equals(this.created, other.created) &&
               Objects.equals(this.trackingId, other.trackingId);
    }

    @Override
    public String toString() {
        return "ItemEntity{" + "agencyId=" + agencyId + ", bibliographicRecordId=" + bibliographicRecordId + ", issueId=" + issueId + ", itemId=" + itemId + ", branch=" + branch + ", branchId=" + branchId + ", department=" + department + ", location=" + location + ", subLocation=" + subLocation + ", circulationRule=" + circulationRule + ", accessionDate=" + accessionDate + ", loanRestriction=" + loanRestriction + ", status=" + status + ", modified=" + modified + ", created=" + created + ", trackingId=" + trackingId + '}';
    }

}
