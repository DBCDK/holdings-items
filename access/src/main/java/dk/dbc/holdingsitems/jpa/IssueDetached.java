package dk.dbc.holdingsitems.jpa;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
}
