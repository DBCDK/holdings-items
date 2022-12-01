package dk.dbc.holdingsitems.content.response;

import java.util.Map;

public class AgencyHoldingsItemsStatusCountResponse {
    public final Map<String, Long> statusCounts;
    public final String trackingId;


    public AgencyHoldingsItemsStatusCountResponse(Map<String, Long> statusCountsByAgency, String trackingId) {
        this.statusCounts = statusCountsByAgency;
        this.trackingId = trackingId;
    }
}
