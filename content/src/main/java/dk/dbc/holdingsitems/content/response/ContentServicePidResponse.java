package dk.dbc.holdingsitems.content.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import dk.dbc.holdingsitems.RecordCollection;

import java.util.Map;

@JsonSerialize(using = ContentServicePidResponseSerializer.class)
public class ContentServicePidResponse {
    private String trackingId;
    private Map<String, RecordCollection> holdingsMap;

    public ContentServicePidResponse(String trackingId, Map<String, RecordCollection> holdingsMap) {
        this.trackingId = ContentServiceItemResponse.generateTrackingIdIfNullOrEmpty(trackingId);
        this.holdingsMap = holdingsMap;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public Map<String, RecordCollection> getHoldingsMap() {
        return holdingsMap;
    }
}