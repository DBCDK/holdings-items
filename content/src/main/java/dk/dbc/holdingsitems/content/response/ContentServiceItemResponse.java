package dk.dbc.holdingsitems.content.response;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.UUID;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class ContentServiceItemResponse {
    public String trackingId;
    public List<CompleteItemFull> holdings;

    public ContentServiceItemResponse(String trackingId, List<CompleteItemFull> holdingsItems) {
        this.trackingId = generateTrackingIdIfNullOrEmpty(trackingId);
        this.holdings = holdingsItems;
    }

    protected static String generateTrackingIdIfNullOrEmpty(String trackingId) {
        return trackingId == null || trackingId.isEmpty() ? UUID.randomUUID().toString() : trackingId;
    }
}
