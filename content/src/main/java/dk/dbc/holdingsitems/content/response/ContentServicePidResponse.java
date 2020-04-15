package dk.dbc.holdingsitems.content.response;

import dk.dbc.holdingsitems.jpa.ItemEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class ContentServicePidResponse {
    public String trackingId;
    public Map<String, List<ResponseHoldingEntity>> holdings;

    public ContentServicePidResponse(String trackingId, Map<String, Iterable<ItemEntity>> holdingsMap) {
        this.trackingId = ContentServiceItemResponse.generateTrackingIdIfNullOrEmpty(trackingId);
        Map<String, List<ResponseHoldingEntity>> responseMap = new HashMap<>();
        Iterator<Map.Entry<String, Iterable<ItemEntity>>> itr = holdingsMap.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, Iterable<ItemEntity>> entry = itr.next();
            responseMap.put(entry.getKey(), ResponseHoldingEntity.listFromItems(entry.getValue()));
        }
        holdings = responseMap;
    }

}
