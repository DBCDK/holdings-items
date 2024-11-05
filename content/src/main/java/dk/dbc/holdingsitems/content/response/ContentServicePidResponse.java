package dk.dbc.holdingsitems.content.response;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Map;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class ContentServicePidResponse {

    public String trackingId;
    public Map<String, List<CompleteItemFull>> holdings;

    public ContentServicePidResponse(String trackingId, Map<String, List<CompleteItemFull>> holdingsMap) {
        this.trackingId = ContentServiceItemResponse.generateTrackingIdIfNullOrEmpty(trackingId);
        holdings = holdingsMap;
    }
}
