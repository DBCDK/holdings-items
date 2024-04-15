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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.LockModeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

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
@SuppressWarnings("PMD.UnusedPrivateField")
@SuppressFBWarnings("EI_EXPOSE_REP")
public class IssueEntity implements Serializable {

    private static final long serialVersionUID = 1089023457634768914L;

    @EmbeddedId
    private IssueKey pk;

    // Mirrors of values from IssueKey
    // Needed for EntityManager.createQuery to access these fields
    // Primary Key
    @Column(updatable = false, nullable = false)
    protected int agencyId;

    @Column(updatable = false, nullable = false)
    protected String bibliographicRecordId;

    @Column(updatable = false, nullable = false)
    protected String issueId;

    // Data fields
    @Column(nullable = false)
    protected String issueText;

    @Column
    protected Date expectedDelivery;

    @Column(nullable = false)
    protected int readyForLoan;

    @Column(nullable = false)
    protected Timestamp complete;

    @Column(nullable = false)
    protected Timestamp modified;

    @Column(nullable = false)
    protected Timestamp created;

    @Column(nullable = false)
    protected Timestamp updated;

    @Column(nullable = false)
    protected String trackingId;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "owner", orphanRemoval = true, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    protected Set<ItemEntity> items;

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
    private int version;

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

    /**
     * persist or merge depending on which is appropriate
     */
    public void save() {
        if (persist) {
            em.persist(this);
        } else {
            em.merge(this);
        }
        persist = false;
        items.forEach(i -> i.persist = false);
    }

    public boolean isEmpty() {
        return items.isEmpty();
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
        if (!item.isNew()) {
            em.remove(item);
        }
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

    // only PRIMARY KEY fields for hash-code
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + this.agencyId;
        hash = 79 * hash + Objects.hashCode(this.bibliographicRecordId);
        hash = 79 * hash + Objects.hashCode(this.issueId);
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
        return getClass().getSimpleName() + "{" + "agencyId=" + agencyId + ", bibliographicRecordId=" + bibliographicRecordId + ", issueId=" + issueId + ", issueText=" + issueText + ", expectedDelivery=" + expectedDelivery + ", readyForLoan=" + readyForLoan + ", complete=" + complete + ", modified=" + modified + ", created=" + created + ", updated=" + updated + ", trackingId=" + trackingId + ", items=" + items + '}';
    }
}
