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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import static java.util.Collections.EMPTY_SET;

/**
 * Database mapping of bibliographicItem table
 * <p>
 * This should never be removed from the database even in case that all issues
 * are gone. The firstAccessionDate should survive is an issue is resurrected.
 *
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Entity
@Table(name = "bibliographicItem")
public class BibliographicItemEntity implements Serializable {

    private static final long serialVersionUID = 1089023457634768914L;

    @EmbeddedId
    @SuppressWarnings("PMD.UnusedPrivateField")
    private final BibliographicItemKey pk;

    // Mirrors of values from IssueKey
    // Needed for EntityManager.createQuery to access these fields
    // Primary Key
    @Column(updatable = false, nullable = false)
    private int agencyId;

    @Column(updatable = false, nullable = false)
    private String bibliographicRecordId;

    @Column(nullable = false)
    private String note;

    @Column(nullable = false)
    private Date firstAccessionDate;

    @Column(nullable = false)
    private Timestamp modified;

    @Column(nullable = false)
    private String trackingId;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "owner", orphanRemoval = true, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private Set<IssueEntity> issues;

    @Transient
    transient boolean persist;

    @Version
    int version;

    @Transient
    transient EntityManager em;

    @Transient
    transient boolean optimisticForceIncrement; // If entities fetched by this entity should be optimistic_force_increment locked

    public static BibliographicItemEntity from(EntityManager em, int agencyId, String bibliographicRecordId, Instant modified, LocalDate firstAccessionDate) {
        return from(em, new BibliographicItemKey(agencyId, bibliographicRecordId), modified, firstAccessionDate, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    }

    public static BibliographicItemEntity fromUnLocked(EntityManager em, int agencyId, String bibliographicRecordId, Instant modified, LocalDate firstAccessionDate) {
        return from(em, new BibliographicItemKey(agencyId, bibliographicRecordId), modified, firstAccessionDate, LockModeType.NONE);
    }

    public static BibliographicItemEntity from(EntityManager em, BibliographicItemKey key, Instant modified, LocalDate firstAccessionDate) {
        return from(em, key, modified, firstAccessionDate, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    }

    public static BibliographicItemEntity fromUnLocked(EntityManager em, BibliographicItemKey key, Instant modified, LocalDate firstAccessionDate) {
        return from(em, key, modified, firstAccessionDate, LockModeType.NONE);
    }

    private static BibliographicItemEntity from(EntityManager em, BibliographicItemKey key, Instant modified, LocalDate firstAccessionDate, LockModeType lock) {
        BibliographicItemEntity entity = em.find(BibliographicItemEntity.class, key, lock);
        if (entity == null) {
            entity = new BibliographicItemEntity(key.getAgencyId(), key.getBibliographicRecordId());
            entity.setFirstAccessionDate(firstAccessionDate);
            entity.setNote("");
            entity.setModified(modified);
        }
        entity.em = em;
        entity.optimisticForceIncrement = lock == LockModeType.OPTIMISTIC_FORCE_INCREMENT;
        return entity;
    }

    public BibliographicItemEntity() {
        this.pk = new BibliographicItemKey();
        this.persist = false;
    }

    BibliographicItemEntity(int agencyId, String bibliographicRecordId) {
        this.pk = new BibliographicItemKey();
        this.agencyId = agencyId;
        this.bibliographicRecordId = bibliographicRecordId;
        this.persist = true;
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
        issues.forEach(IssueEntity::persisted);
    }

    /**
     * Stream over all Item entities that this collection covers
     *
     * @return stream of all related items
     */
    public Stream<IssueEntity> stream() {
        if (issues == null)
            return EMPTY_SET.stream();
        return issues.stream();
    }

    public IssueEntity issue(String issueId, Instant modified) {
        IssueEntity issue = em.find(IssueEntity.class, new IssueKey(agencyId, bibliographicRecordId, issueId),
                                    optimisticForceIncrement ? LockModeType.OPTIMISTIC_FORCE_INCREMENT : LockModeType.NONE);
        if (issue == null) {
            if (issues == null)
                issues = new HashSet<>();
            issue = new IssueEntity(this, issueId);
            issue.setComplete(modified);
            issue.setCreated(modified);
            issue.setModified(modified);
            issue.setTrackingId(trackingId);
            issues.add(issue);
        }
        issue.optimisticForceIncrement = optimisticForceIncrement;
        issue.em = em;
        issue.owner = this;
        return issue;
    }

    public void removeIssue(IssueEntity issue) {
        issues.remove(issue);
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

    public String getNote() {
        return note;
    }

    public BibliographicItemEntity setNote(String note) {
        this.note = note;
        return this;
    }

    public LocalDate getFirstAccessionDate() {
        return firstAccessionDate.toLocalDate();
    }

    /**
     * Only set it it move the date back in time
     *
     * @param accessionDate When an item was accessioned
     * @return self
     */
    public BibliographicItemEntity setFirstAccessionDate(LocalDate accessionDate) {
        if (accessionDate != null &&
            ( this.firstAccessionDate == null ||
              firstAccessionDate.toLocalDate().isAfter(accessionDate) )) {
            this.firstAccessionDate = Date.valueOf(accessionDate);
        }
        return this;
    }

    public Instant getModified() {
        return modified.toInstant();
    }

    public BibliographicItemEntity setModified(Instant modified) {
        this.modified = Timestamp.from(modified);
        return this;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public BibliographicItemEntity setTrackingId(String trackingId) {
        this.trackingId = trackingId;
        return this;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + this.agencyId;
        hash = 79 * hash + Objects.hashCode(this.bibliographicRecordId);
        hash = 79 * hash + Objects.hashCode(this.note);
        hash = 79 * hash + Objects.hashCode(this.firstAccessionDate);
        hash = 79 * hash + Objects.hashCode(this.modified);
        hash = 79 * hash + Objects.hashCode(this.trackingId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final BibliographicItemEntity other = (BibliographicItemEntity) obj;
        return this.agencyId == other.agencyId &&
               Objects.equals(this.bibliographicRecordId, other.bibliographicRecordId) &&
               Objects.equals(this.note, other.note) &&
               Objects.equals(this.firstAccessionDate, other.firstAccessionDate) &&
               Objects.equals(this.modified, other.modified) &&
               Objects.equals(this.trackingId, other.trackingId);
    }

    @Override
    public String toString() {
        return "BibliographicItemEntity{" + "agencyId=" + agencyId + ", bibliographicRecordId=" + bibliographicRecordId + ", note=" + note + ", firstAccessionDate=" + firstAccessionDate + ", modified=" + modified + ", trackingId=" + trackingId + ", items=" + issues + '}';
    }

}
