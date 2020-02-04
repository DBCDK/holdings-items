package dk.dbc.holdingsitems.content.response;

import dk.dbc.holdingsitems.RecordCollection;

import java.util.Map;

public class ContentServicePidResponse {
    private String trackingId;
    private Map<String, RecordCollection> holdingsMap;

    public ContentServicePidResponse(String trackingId, Map<String, RecordCollection> holdingsMap) {
        this.trackingId = trackingId;
        this.holdingsMap = holdingsMap;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public Map<String, RecordCollection> getHoldingsMap() {
        return holdingsMap;
    }
}