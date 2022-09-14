package dk.dbc.holdingsitems.jpa;

import dk.dbc.holdingsitems.content_dto.CompleteIssue;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
public class IssueDetached extends IssueEntity {

    private HashMap<String, ItemEntity> itemMap;

    IssueDetached(IssueEntity entity) {
        this.agencyId = entity.agencyId;
        this.bibliographicRecordId = entity.bibliographicRecordId;
        this.issueId = entity.issueId;
        this.issueText = entity.issueText;
        this.expectedDelivery = entity.expectedDelivery;
        this.readyForLoan = entity.readyForLoan;
        this.complete = entity.complete;
        this.modified = entity.modified;
        this.created = entity.created;
        this.updated = entity.updated;
        this.trackingId = entity.trackingId;

        this.itemMap = new HashMap<>();
        if (entity.items != null) {
            entity.items.forEach(item -> this.itemMap.put(item.getItemId(), item));
        }
        this.items = new HashSet<>(this.itemMap.values());
    }

    static IssueDetached merge(IssueDetached self, IssueDetached other) {
        self.readyForLoan += other.readyForLoan;
        if (self.expectedDelivery == null || other.expectedDelivery != null && self.expectedDelivery.after(other.expectedDelivery)) {
            self.expectedDelivery = other.expectedDelivery;
        }
        if (self.issueText.isEmpty()) {
            self.issueText = other.issueText;
        }
        other.itemMap.forEach(self.itemMap::putIfAbsent);
        self.items = new HashSet<>(self.itemMap.values());
        return self;
    }

    public Set<ItemEntity> getItems() {
        return Collections.unmodifiableSet(items);
    }

    public CompleteIssue toCompleteIssue() {
        CompleteIssue that = new CompleteIssue();
        that.issueId = getIssueId();
        that.issueText = getIssueText();
        that.expectedDelivery = getExpectedDelivery() != null ? getExpectedDelivery().toString() : null;
        that.readyForLoan = getReadyForLoan();
        that.items = stream()
                .sorted(Comparator.comparing(ItemEntity::getItemId, new VersionSort()))
                .map(ItemEntity::toCompleteItem)
                .collect(Collectors.toList());
        return that;
    }
}
