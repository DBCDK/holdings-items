package dk.dbc.holdingsitems.content.response;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import dk.dbc.holdingsitems.jpa.ItemEntity;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@JsonSerialize(using = ContentServiceItemResponseSerializer.class)
public class ContentServiceItemResponse {
    private String trackingId;
    private List<ItemEntity> holdings;

    public ContentServiceItemResponse(List<ItemEntity> holdings, String trackingId) {
        this.holdings = holdings;
        this.trackingId = generateTrackingIdIfNullOrEmpty(trackingId);
    }

    public ContentServiceItemResponse(ItemEntity holding, String trackingId) {
        this.holdings = Arrays.asList(new ItemEntity[] { holding });
        this.trackingId = generateTrackingIdIfNullOrEmpty(trackingId);
    }

    protected static String generateTrackingIdIfNullOrEmpty(String trackingId) {
        return (trackingId == null || trackingId.isEmpty()) ? UUID.randomUUID().toString() : trackingId;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public List<ItemEntity> getHoldings() {
        return holdings;
    }
}