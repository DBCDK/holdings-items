package dk.dbc.holdingsitems.content.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import dk.dbc.holdingsitems.RecordCollection;

import java.util.UUID;

@JsonSerialize(using = ContentServiceItemResponseSerializer.class)
public class ContentServiceItemResponse {
    private String trackingId;
    private RecordCollection recordCollection;

    public ContentServiceItemResponse(String trackingId, RecordCollection recordCollection) {
        this.trackingId = generateTrackingIdIfNullOrEmpty(trackingId);
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