package dk.dbc.holdingsitems.content.response;

import dk.dbc.holdingsitems.RecordCollection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.UUID;

public class ContentServiceItemResponse {
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public String trackingId;
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public List<ResponseHoldingEntity> holdings;

    public ContentServiceItemResponse(String trackingId, RecordCollection recordCollection) {
        this.trackingId = generateTrackingIdIfNullOrEmpty(trackingId);
        this.holdings = ResponseHoldingEntity.listFromRecordCollection(recordCollection);
    }

    protected static String generateTrackingIdIfNullOrEmpty(String trackingId) {
        return trackingId == null || trackingId.isEmpty() ? UUID.randomUUID().toString() : trackingId;
    }

}