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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

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
@SuppressWarnings("PMD.UnusedPrivateField")
public class BibliographicItemEntity implements Serializable {

    private static final long serialVersionUID = 1089023457634768914L;

    @EmbeddedId
    private BibliographicItemKey pk;

    // Mirrors of values from IssueKey
    // Needed for EntityManager.createQuery to access these fields
    // Primary Key
    @Column(updatable = false, nullable = false)
    protected int agencyId;

    @Column(updatable = false, nullable = false)
    protected String bibliographicRecordId;

    @Column(nullable = false)
    protected String note;

    @Column(nullable = false)
    protected Date firstAccessionDate;

    @Column(nullable = false)
    protected Timestamp modified;

    @Column(nullable = false)
    protected String trackingId;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "owner", orphanRemoval = true, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    protected Set<IssueEntity> issues;

    @Transient
    private transient boolean persist;

    @Version
    private int version;

    @Transient
    private transient EntityManager em;

    @Transient
    private transient boolean pessimisticForceIncrement; // If entities fetched by this entity should be pessimistic_force_increment locked

    public static BibliographicItemEntity from(EntityManager em, int agencyId, String bibliographicRecordId, Instant modified, LocalDate firstAccessionDate) {
        return from(em, new BibliographicItemKey(agencyId, bibliographicRecordId), modified, firstAccessionDate, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
    }

    public static BibliographicItemEntity fromUnLocked(EntityManager em, int agencyId, String bibliographicRecordId) {
        return fromUnLocked(em, new BibliographicItemKey(agencyId, bibliographicRecordId));
    }

    public static BibliographicItemEntity from(EntityManager em, BibliographicItemKey key, Instant modified, LocalDate firstAccessionDate) {
        return from(em, key, modified, firstAccessionDate, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
    }

    public static BibliographicItemEntity fromUnLocked(EntityManager em, BibliographicItemKey key) {
        BibliographicItemEntity entity = em.find(BibliographicItemEntity.class, key, LockModeType.NONE);
        if (entity != null) {
            entity.em = em;
            entity.pessimisticForceIncrement = false;
        }
        return entity;
    }

    public static BibliographicItemDetached detachedWithSuperseded(EntityManager em, int agencyId, String bibliographicRecordId) {
        SupersedesEntity superseded = em.find(SupersedesEntity.class, bibliographicRecordId);
        if (superseded != null) {
            return null;
        }
        BibliographicItemEntity entity = fromUnLocked(em, agencyId, bibliographicRecordId);
        if (entity != null) {
            return BibliographicItemDetached.detached(em, entity);
        } else {
            return BibliographicItemDetached.detached(em, agencyId, bibliographicRecordId);
        }
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
        entity.pessimisticForceIncrement = lock == LockModeType.PESSIMISTIC_FORCE_INCREMENT;
        return entity;
    }

    public BibliographicItemEntity() {
        this.pk = new BibliographicItemKey();
        this.persist = false;
        this.issues = new HashSet<>();
    }

    BibliographicItemEntity(int agencyId, String bibliographicRecordId) {
        this.pk = new BibliographicItemKey();
        this.agencyId = agencyId;
        this.bibliographicRecordId = bibliographicRecordId;
        this.persist = true;
        this.issues = new HashSet<>();
    }

    /**
     * persist or merge depending on which is appropriate
     */
    public void save() {
        removeEmptyIssues();
        if (persist) {
            em.persist(this);
        } else {
            em.merge(this);
        }
        persist = false;
        issues.forEach(is -> {
            is.em = em;
            is.save();
        });
        em.flush();
    }

    private void removeEmptyIssues() {
        for (Iterator<IssueEntity> issueIter = issues.iterator() ; issueIter.hasNext() ;) {
            IssueEntity issue = issueIter.next();
            if (issue.isEmpty()) {
                issue.owner = null;
                issueIter.remove();
                if (!issue.isNew()) {
                    em.remove(issue);
                }
            }
        }
    }

    /**
     * Stream over all Item entities that this collection covers
     *
     * @return stream of all related items
     */
    public Stream<IssueEntity> stream() {
        if (issues == null)
            return EMPTY_SET.stream();
        issues.forEach(issue -> issue.em = em);
        return issues.stream();
    }

    public IssueEntity issue(String issueId, Instant modified) {
        IssueKey key = new IssueKey(agencyId, bibliographicRecordId, issueId);
        IssueEntity issue = em.find(IssueEntity.class, key,
                                    pessimisticForceIncrement ? LockModeType.PESSIMISTIC_FORCE_INCREMENT : LockModeType.NONE);
        if (issue == null) {
            issue = new IssueEntity(this, issueId);
            issue.setComplete(modified);
            issue.setCreated(modified);
            issue.setModified(modified);
            issue.setUpdated(modified);
            issue.setTrackingId(trackingId);
        } else {
            issue.persist = false;
        }
        issues.add(issue);
        issue.pessimisticForceIncrement = pessimisticForceIncrement;
        issue.em = em;
        issue.owner = this;
        return issue;
    }

    public void removeIssue(IssueEntity issue) {
        issues.remove(issue);
    }

    public boolean isEmpty() {
        if (!issues.isEmpty()) {
            for (IssueEntity issue : issues) {
                if (!issue.isEmpty())
                    return false;
            }
        }
        return true;
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
        return getClass().getSimpleName() + "{" + "agencyId=" + agencyId + ", bibliographicRecordId=" + bibliographicRecordId + ", note=" + note + ", firstAccessionDate=" + firstAccessionDate + ", modified=" + modified + ", trackingId=" + trackingId + ", issues=" + issues + '}';
    }
}
