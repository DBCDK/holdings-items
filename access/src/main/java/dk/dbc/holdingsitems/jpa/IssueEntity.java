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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.LockModeType;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;


/**
 * Database mapping of holdingsitemscollection table
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Entity
@Table(name = "issue")
@NamedQueries({
    @NamedQuery(name = "byAgencyBibliographic",
                query = "SELECT h" +
                        " FROM IssueEntity h" +
                        " WHERE h.agencyId = :agencyId" +
                        "  AND h.bibliographicRecordId = :bibliographicRecordId")
})
public class IssueEntity implements Serializable {

    private static final long serialVersionUID = 1089023457634768914L;

    @EmbeddedId
    @SuppressWarnings("PMD.UnusedPrivateField")
    private final IssueKey pk;

    // Mirrors of values from IssueKey
    // Needed for EntityManager.createQuery to access these fields
    // Primary Key
    @Column(updatable = false, nullable = false)
    private int agencyId;

    @Column(updatable = false, nullable = false)
    private String bibliographicRecordId;

    @Column(updatable = false, nullable = false)
    private String issueId;

    // Data fields
    @Column(nullable = false)
    private String issueText;

    @Column
    private Date expectedDelivery;

    @Column(nullable = false)
    private int readyForLoan;

    @Column(nullable = false)
    private Timestamp complete;

    @Column(nullable = false)
    private Timestamp modified;

    @Column(nullable = false)
    private Timestamp created;

    @Column(nullable = false)
    private Timestamp updated;

    @Column(nullable = false)
    private String trackingId;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "owner", orphanRemoval = true, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private Set<ItemEntity> items;

    @MapsId("collection") // Refers to pk(IssueKey).collection
    // Columns needs to be insertable=false, updatable=false to not collide with mirrored fields
    @JoinColumns({
        @JoinColumn(name = "agencyId", referencedColumnName = "agencyId",
                    insertable = false, updatable = false),
        @JoinColumn(name = "bibliographicRecordId", referencedColumnName = "bibliographicRecordId",
                    insertable = false, updatable = false)
    })
    @ManyToOne(cascade = CascadeType.ALL)
    BibliographicItemEntity owner;

    public BibliographicItemEntity getOwner() {
        return owner;
    }

    @Version
    int version;

    @Transient
    transient boolean persist;

    @Transient
    transient EntityManager em;

    @Transient
    transient boolean pessimisticForceIncrement; // If entities fetched by this entity should be pessimistic_force_increment locked

    public static List<IssueEntity> byAgencyBibliographic(EntityManager em, int agencyId, String bibliographicRecordId) {
        List<IssueEntity> list = em.createNamedQuery("byAgencyBibliographic", IssueEntity.class)
                .setParameter("agencyId", agencyId)
                .setParameter("bibliographicRecordId", bibliographicRecordId)
                .getResultList();
        list.forEach(e -> e.em = em);
        return list;
    }

    public IssueEntity() {
        this.pk = new IssueKey();
        this.persist = false;
        this.items = new HashSet<>();
    }

    IssueEntity(BibliographicItemEntity owner, String issueId) {
        this.pk = new IssueKey();
        this.agencyId = owner.getAgencyId();
        this.bibliographicRecordId = owner.getBibliographicRecordId();
        this.issueId = issueId;
        this.owner = owner;
        this.persist = true;
        this.items = new HashSet<>();
    }

    public static IssueEntity from(EntityManager em, BibliographicItemEntity owner, String issueId) {
        IssueEntity res = em.find(IssueEntity.class, new IssueKey(owner.getAgencyId(), owner.getBibliographicRecordId(), issueId));
        if (res == null) {
            res = new IssueEntity(owner, issueId);
            res.setComplete(Instant.now());
            res.setModified(Instant.now());
            res.setCreated(Instant.now());
            res.setUpdated(Instant.now());
        }
        res.em = em;
        res.pessimisticForceIncrement = owner.pessimisticForceIncrement;
        return res;
    }

    /**
     * persist or merge depending on which is appropriate
     */
    public void save() {
        if (persist) {
            em.persist(this);
        } else {
            em.merge(this);
        }
        persisted();
    }

    void persisted() {
        persist = false;
        items.forEach(ItemEntity::persisted);
    }

    /**
     * Stream over all Item entities that this collection covers
     *
     * @return stream of all related items
     */
    public Stream<ItemEntity> stream() {
        return items.stream();
    }

    /**
     * Iterator for all Item entities that this collection covers
     *
     * @return iterator of all related items
     */
    public Iterator<ItemEntity> iterator() {
        return items.iterator();
    }

    /**
     * Acquire a Item from the database cache, or create one and attach it to
     * this collection.
     *
     * @param itemId   name of the item
     * @param modified if created use this for timestamp
     * @return related item (remember to set all values, there's no defaults if
     *         it is newly created)
     */
    public ItemEntity item(String itemId, Instant modified) {
        ItemKey key = new ItemKey(agencyId, bibliographicRecordId, issueId, itemId);
        ItemEntity item = em.find(ItemEntity.class, key,
                                  pessimisticForceIncrement ? LockModeType.PESSIMISTIC_FORCE_INCREMENT : LockModeType.NONE);
        if (item == null) {
            item = new ItemEntity(this, itemId);
            item.setCreated(Instant.now());
            item.setModified(modified);
            item.setTrackingId(trackingId);
        } else {
            item.persist = false;
        }
        items.add(item);
        item.owner = this;
        return item;
    }

    public void removeItem(ItemEntity item) {
        items.remove(item);
        item.owner = null;
        em.remove(item);
    }

    public boolean isNew() {
        return this.persist;
    }

    /**
     * Propagate it up through the structure
     *
     * @param accessionDate When something was accessioned
     */
    public void setFirstAccessionDate(LocalDate accessionDate) {
        owner.setFirstAccessionDate(accessionDate);
    }

    // Accessors (Immutable)
    public int getAgencyId() {
        return agencyId;
    }

    public String getBibliographicRecordId() {
        return bibliographicRecordId;
    }

    public String getIssueId() {
        return issueId;
    }

    // Accessors
    public String getIssueText() {
        return issueText;
    }

    public IssueEntity setIssueText(String issueText) {
        this.issueText = issueText;
        return this;
    }

    public LocalDate getExpectedDelivery() {
        return expectedDelivery == null ? null : expectedDelivery.toLocalDate();
    }

    public IssueEntity setExpectedDelivery(LocalDate expectedDelivery) {
        this.expectedDelivery = expectedDelivery == null ? null : Date.valueOf(expectedDelivery);
        return this;
    }

    public int getReadyForLoan() {
        return readyForLoan;
    }

    public IssueEntity setReadyForLoan(int readyForLoan) {
        this.readyForLoan = readyForLoan;
        return this;
    }

    public Instant getComplete() {
        return complete.toInstant();
    }

    public IssueEntity setComplete(Instant complete) {
        this.complete = Timestamp.from(complete);
        return this;
    }

    public Instant getModified() {
        return modified.toInstant();
    }

    public IssueEntity setModified(Instant modified) {
        this.modified = Timestamp.from(modified);
        return this;
    }

    public Instant getCreated() {
        return created.toInstant();
    }

    public IssueEntity setCreated(Instant created) {
        this.created = Timestamp.from(created);
        return this;
    }

    public Instant getUpdated() {
        return updated.toInstant();
    }

    public IssueEntity setUpdated(Instant updated) {
        this.updated = Timestamp.from(updated);
        return this;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public IssueEntity setTrackingId(String trackingId) {
        this.trackingId = trackingId;
        return this;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + this.agencyId;
        hash = 79 * hash + Objects.hashCode(this.bibliographicRecordId);
        hash = 79 * hash + Objects.hashCode(this.issueId);
        hash = 79 * hash + Objects.hashCode(this.issueText);
        hash = 79 * hash + Objects.hashCode(this.expectedDelivery);
        hash = 79 * hash + this.readyForLoan;
        hash = 79 * hash + Objects.hashCode(this.complete);
        hash = 79 * hash + Objects.hashCode(this.modified);
        hash = 79 * hash + Objects.hashCode(this.created);
        hash = 79 * hash + Objects.hashCode(this.updated);
        hash = 79 * hash + Objects.hashCode(this.trackingId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final IssueEntity other = (IssueEntity) obj;
        return this.agencyId == other.agencyId &&
               Objects.equals(this.bibliographicRecordId, other.bibliographicRecordId) &&
               Objects.equals(this.issueId, other.issueId) &&
               Objects.equals(this.issueText, other.issueText) &&
               Objects.equals(this.expectedDelivery, other.expectedDelivery) &&
               this.readyForLoan == other.readyForLoan &&
               Objects.equals(this.complete, other.complete) &&
               Objects.equals(this.modified, other.modified) &&
               Objects.equals(this.created, other.created) &&
               Objects.equals(this.updated, other.updated) &&
               Objects.equals(this.trackingId, other.trackingId);
    }

    @Override
    public String toString() {
        return "IssueEntity{" + "agencyId=" + agencyId + ", bibliographicRecordId=" + bibliographicRecordId + ", issueId=" + issueId + ", issueText=" + issueText + ", expectedDelivery=" + expectedDelivery + ", readyForLoan=" + readyForLoan + ", complete=" + complete + ", modified=" + modified + ", created=" + created + ", updated=" + updated + ", trackingId=" + trackingId + ", items=" + items + '}';
    }

}
