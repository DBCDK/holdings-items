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
import javax.persistence.LockModeType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import static java.util.Collections.EMPTY_SET;

/**
 * Database mapping of holdingsitemscollection table
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Entity
@Table(name = "holdingsitemscollection")
@NamedQueries({
    @NamedQuery(name = "byAgencyBibliographic",
                query = "SELECT h" +
                        " FROM HoldingsItemsCollectionEntity h" +
                        " WHERE h.agencyId = :agencyId" +
                        "  AND h.bibliographicRecordId = :bibliographicRecordId")
})
public class HoldingsItemsCollectionEntity implements Serializable {

    private static final long serialVersionUID = 1089023457634768914L;

    @EmbeddedId
    private final HoldingsItemsCollectionKey pk;

    // Mirrors of values from HoldingsItemsCollectionKey
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
    private String note;

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

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "owner", orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<HoldingsItemsItemEntity> items;

    @Transient
    private transient boolean persist;

    @Transient
    private transient EntityManager em;

    public static HoldingsItemsCollectionEntity from(EntityManager em, int agencyId, String bibliographicRecordId, String issueId, Instant modified) {
        return from(em, new HoldingsItemsCollectionKey(agencyId, bibliographicRecordId, issueId), modified);
    }

    public static HoldingsItemsCollectionEntity from(EntityManager em, HoldingsItemsCollectionKey key, Instant modified) {
        HoldingsItemsCollectionEntity entity = em.find(HoldingsItemsCollectionEntity.class, key, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) {
            entity = new HoldingsItemsCollectionEntity(key.getAgencyId(), key.getBibliographicRecordId(), key.getIssueId());
            entity.setIssueText("");
            entity.setNote("");
            entity.setComplete(modified);
            entity.setCreated(modified);
        }
        entity.em = em;
        return entity;
    }

    public static List<HoldingsItemsCollectionEntity> byAgencyBibliographic(EntityManager em, int agencyId, String bibliographicRecordId) {
        List<HoldingsItemsCollectionEntity> list = em.createNamedQuery("byAgencyBibliographic", HoldingsItemsCollectionEntity.class)
                .setParameter("agencyId", agencyId)
                .setParameter("bibliographicRecordId", bibliographicRecordId)
                .getResultList();
        list.forEach(e -> e.em = em);
        return list;
    }

    public HoldingsItemsCollectionEntity() {
        this.pk = new HoldingsItemsCollectionKey();
        this.persist = false;
    }

    private HoldingsItemsCollectionEntity(int agencyId, String bibliographicRecordId, String issueId) {
        this.pk = new HoldingsItemsCollectionKey();
        this.agencyId = agencyId;
        this.bibliographicRecordId = bibliographicRecordId;
        this.issueId = issueId;
        this.persist = true;
    }

    /**
     * persist or merge depending on which is appropriate
     */
    public void save() {
        if (persist) {
            em.persist(this);
            if (items != null)
                items.forEach(i -> i.persist = false);
            persist = false;
        } else {
            em.merge(this);
        }
    }

    /**
     * Stream over all Item entities that this collection covers
     *
     * @return stream of all related items
     */
    public Stream<HoldingsItemsItemEntity> stream() {
        if (items == null)
            return EMPTY_SET.stream();
        return items.stream();
    }

    /**
     * Iterator for all Item entities that this collection covers
     *
     * @return iterator of all related items
     */
    public Iterator<HoldingsItemsItemEntity> iterator() {
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
    public HoldingsItemsItemEntity item(String itemId, Instant modified) {
        HoldingsItemsItemEntity item = em.find(HoldingsItemsItemEntity.class, new HoldingsItemsItemKey(pk, itemId));
        if (item == null) {
            if (items == null)
                items = new HashSet<>();
            item = new HoldingsItemsItemEntity(this, itemId);
            item.setCreated(modified);
            item.setModified(modified);
            items.add(item);
        }
        return item;
    }

    public void removeItem(HoldingsItemsItemEntity item) {
        items.remove(item);
    }

    public boolean isNew() {
        return this.persist;
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

    public HoldingsItemsCollectionEntity setIssueText(String issueText) {
        this.issueText = issueText;
        return this;
    }

    public LocalDate getExpectedDelivery() {
        return expectedDelivery == null ? null : expectedDelivery.toLocalDate();
    }

    public HoldingsItemsCollectionEntity setExpectedDelivery(LocalDate expectedDelivery) {
        this.expectedDelivery = expectedDelivery == null ? null : Date.valueOf(expectedDelivery);
        return this;
    }

    public int getReadyForLoan() {
        return readyForLoan;
    }

    public HoldingsItemsCollectionEntity setReadyForLoan(int readyForLoan) {
        this.readyForLoan = readyForLoan;
        return this;
    }

    public String getNote() {
        return note;
    }

    public HoldingsItemsCollectionEntity setNote(String note) {
        this.note = note;
        return this;
    }

    public Instant getComplete() {
        return complete.toInstant();
    }

    public HoldingsItemsCollectionEntity setComplete(Instant complete) {
        this.complete = Timestamp.from(complete);
        return this;
    }

    public Instant getModified() {
        return modified.toInstant();
    }

    public HoldingsItemsCollectionEntity setModified(Instant modified) {
        this.modified = Timestamp.from(modified);
        return this;
    }

    public Instant getCreated() {
        return created.toInstant();
    }

    public HoldingsItemsCollectionEntity setCreated(Instant created) {
        this.created = Timestamp.from(created);
        return this;
    }

    public Instant getUpdated() {
        return updated.toInstant();
    }

    public HoldingsItemsCollectionEntity setUpdated(Instant updated) {
        this.updated = Timestamp.from(updated);
        return this;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public HoldingsItemsCollectionEntity setTrackingId(String trackingId) {
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
        hash = 79 * hash + Objects.hashCode(this.note);
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
        final HoldingsItemsCollectionEntity other = (HoldingsItemsCollectionEntity) obj;
        return this.agencyId == other.agencyId &&
               Objects.equals(this.bibliographicRecordId, other.bibliographicRecordId) &&
               Objects.equals(this.issueId, other.issueId) &&
               this.readyForLoan == other.readyForLoan &&
               Objects.equals(this.issueText, other.issueText) &&
               Objects.equals(this.note, other.note) &&
               Objects.equals(this.trackingId, other.trackingId) &&
               Objects.equals(this.expectedDelivery, other.expectedDelivery) &&
               Objects.equals(this.complete, other.complete) &&
               Objects.equals(this.modified, other.modified) &&
               Objects.equals(this.created, other.created) &&
               Objects.equals(this.updated, other.updated) &&
               Objects.equals(this.items, other.items);
    }

    @Override
    public String toString() {
        return "HoldingsItemsCollectionEntity{" + "agencyId=" + agencyId + ", bibliographicRecordId=" + bibliographicRecordId + ", issueId=" + issueId + ", issueText=" + issueText + ", expectedDelivery=" + expectedDelivery + ", readyForLoan=" + readyForLoan + ", note=" + note + ", complete=" + complete + ", modified=" + modified + ", created=" + created + ", updated=" + updated + ", trackingId=" + trackingId + ", items=" + items + '}';
    }

}
