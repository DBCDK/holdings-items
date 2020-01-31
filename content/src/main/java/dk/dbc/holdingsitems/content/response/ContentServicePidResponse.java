package dk.dbc.holdingsitems.content.response;

import dk.dbc.holdingsitems.jpa.ItemEntity;

import java.util.Map;

public class ContentServicePidResponse {
    private String trackingId;
    private Map<String, ItemEntity> holdingsMap;

    public ContentServicePidResponse(Map<String, ItemEntity> holdingsMap, String trackingId) {
        this.holdingsMap = holdingsMap;
        this.trackingId = ContentServiceItemResponse.generateTrackingIdIfNullOrEmpty(trackingId);
    }

    public String getTrackingId() {
        return trackingId;
    }

    public Map<String, ItemEntity> getHoldingsMap() {
        return holdingsMap;
    }
}