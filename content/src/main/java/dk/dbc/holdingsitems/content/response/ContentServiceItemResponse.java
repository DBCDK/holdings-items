package dk.dbc.holdingsitems.content.response;

import dk.dbc.holdingsitems.jpa.ItemEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.UUID;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class ContentServiceItemResponse {
    public String trackingId;
    public List<ResponseHoldingEntity> holdings;

    public ContentServiceItemResponse(String trackingId, Iterable<ItemEntity> holdingsItems) {
        this.trackingId = generateTrackingIdIfNullOrEmpty(trackingId);
        this.holdings = ResponseHoldingEntity.listFromItems(holdingsItems);
    }

    public static ContentServiceItemResponse generateCompactContentServiceItemResponse(String trackingId, Iterable<ItemEntity> holdingsItems) {
        final ContentServiceItemResponse res = new ContentServiceItemResponse(trackingId, holdingsItems);
        res.holdings.stream().map(he -> {
            he.circulationRule = null;
            he.department = null;
            he.issueText = null;
            he.issueId = null;
            he.itemId = null;
            he.location = null;
            he.note = null;
            he.status = null;
            he.subLocation = null;
            return he;
        });
        return res;
    }

    protected static String generateTrackingIdIfNullOrEmpty(String trackingId) {
        return trackingId == null || trackingId.isEmpty() ? UUID.randomUUID().toString() : trackingId;
    }

}
