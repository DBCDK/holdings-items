package dk.dbc.holdingsitems.content.response;

import dk.dbc.holdingsitems.RecordCollection;

import java.util.UUID;

public class ContentServiceItemResponse {
    private String trackingId;
    private RecordCollection recordCollection;

    public ContentServiceItemResponse(String trackingId, RecordCollection recordCollection) {
        this.trackingId = trackingId;
        this.recordCollection = recordCollection;
    }


    protected static String generateTrackingIdIfNullOrEmpty(String trackingId) {
        return (trackingId == null || trackingId.isEmpty()) ? UUID.randomUUID().toString() : trackingId;
    }

    public String getTrackingId() {
        return trackingId;
    }


    public RecordCollection getRecordCollection() {
        return recordCollection;
    }
}