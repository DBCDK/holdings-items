package dk.dbc.holdingsitems.content.response;

import dk.dbc.holdingsitems.RecordCollection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ContentServicePidResponse {
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public String trackingId;
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public Map<String, List<ResponseHoldingEntity>> holdings;

    public ContentServicePidResponse(String trackingId, Map<String, RecordCollection> holdingsMap) {
        this.trackingId = ContentServiceItemResponse.generateTrackingIdIfNullOrEmpty(trackingId);
        Map<String, List<ResponseHoldingEntity>> responseMap = new HashMap<>();
        Iterator<Map.Entry<String, RecordCollection>> itr = holdingsMap.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, RecordCollection> entry = itr.next();
            responseMap.put(entry.getKey(), ResponseHoldingEntity.listFromRecordCollection(entry.getValue()));
        }
        holdings = responseMap;
    }

}