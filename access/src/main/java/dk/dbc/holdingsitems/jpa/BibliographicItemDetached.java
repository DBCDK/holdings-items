package dk.dbc.holdingsitems.jpa;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;

import static dk.dbc.holdingsitems.jpa.BibliographicItemEntity.fromUnLocked;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
public class BibliographicItemDetached extends BibliographicItemEntity {

    private final HashMap<String, IssueDetached> issueMap;
    private boolean ok;

    public static BibliographicItemDetached detached(EntityManager em, BibliographicItemEntity e) {
        return new BibliographicItemDetached(e.agencyId, e.bibliographicRecordId, e.note, e.firstAccessionDate, e.modified, e.trackingId, e.issues)
                .merge(em)
                .validObject();
    }

    public static BibliographicItemDetached detached(EntityManager em, int agencyId, String bibliographicRecordId) {
        return new BibliographicItemDetached(agencyId, bibliographicRecordId)
                .merge(em)
                .validObject();
    }

    private BibliographicItemDetached(int agencyId, String bibliographicRecordId) {
        this.agencyId = agencyId;
        this.bibliographicRecordId = bibliographicRecordId;
        this.issueMap = new HashMap<>();
        this.ok = false;
    }

    private BibliographicItemDetached(int agencyId, String bibliographicRecordId, String note, Date firstAccessionDate, Timestamp modified, String trackingId, Set<IssueEntity> issues) {
        this.agencyId = agencyId;
        this.bibliographicRecordId = bibliographicRecordId;
        this.note = note;
        this.firstAccessionDate = firstAccessionDate;
        this.modified = modified;
        this.trackingId = trackingId;
        this.issueMap = new HashMap<>();
        if (issues != null) {
            issues.stream().forEach(issue -> issueMap.put(issue.getIssueId(), new IssueDetached(issue)));
        }
        this.ok = true;
    }

    public Collection<IssueDetached> getIssues() {
        return issueMap.values();
    }

    private BibliographicItemDetached merge(EntityManager em) {
        List<BibliographicItemEntity> extraBibItems = SupersedesEntity.bySupersedingNoLock(em, bibliographicRecordId)
                .map(SupersedesEntity::getSuperseded)
                .sorted(new VersionSort().reversed())
                .map(bibId -> fromUnLocked(em, agencyId, bibId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!extraBibItems.isEmpty()) {
            extraBibItems.forEach(extraBib -> {
                mergeFirstAccessionDate(extraBib.getFirstAccessionDate());
                if (extraBib.issues != null && !extraBib.issues.isEmpty()) {
                    this.ok = true;
                    mergeModified(extraBib.getModified());
                    mergeNote(extraBib.getNote());
                    mergeTrackingId(extraBib.getTrackingId());
                    extraBib.issues.forEach(extraIssue -> {
                        issueMap.merge(extraIssue.getIssueId(), new IssueDetached(extraIssue), IssueDetached::merge);
                    });
                }
            });
            if (!ok) {
                // No metadata has been merged, since we found no issues, but we have an extra bib-item
                BibliographicItemEntity otherBibItem = extraBibItems.get(0);
                mergeModified(otherBibItem.getModified());
                mergeNote(otherBibItem.getNote());
                mergeTrackingId(otherBibItem.getTrackingId());
                this.ok = true;
            }
        }
        return this;
    }

    private void mergeFirstAccessionDate(LocalDate firstAccessionDate) {
        if (this.firstAccessionDate == null || getFirstAccessionDate().isAfter(firstAccessionDate)) {
            setFirstAccessionDate(firstAccessionDate);
        }
    }

    private void mergeNote(String note) {
        if (this.note == null || this.note.isEmpty()) {
            setNote(note);
        }
    }

    private void mergeModified(Instant modified) {
        if (this.modified == null || getModified().isAfter(modified))
            setModified(modified);
    }

    private void mergeTrackingId(String trackingId) {
        if (this.trackingId == null) {
            setTrackingId(trackingId);
        }
    }

    private BibliographicItemDetached validObject() {
        if (ok) {
            this.issues = new HashSet<>(issueMap.values());
            return this;
        } else {
            return null;
        }
    }
}
