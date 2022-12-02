package dk.dbc.holdingsitems.content.response;

import java.util.Map;

/**
 * Class to represent object to return to endpoint
 */
public class StatusCountResponse {

    public int agencyId;
    public final Map<String, Long> statusCounts;
    public final String trackingId;


    public StatusCountResponse(int agencyId, Map<String, Long> statusCountsByAgency, String trackingId) {
        this.agencyId = agencyId;
        this.statusCounts = statusCountsByAgency;
        this.trackingId = trackingId;
    }

    @Override
    public String toString() {
        return "StatusCountResponse{" +
                "agencyId=" + agencyId +
                ", statusCounts=" + statusCounts +
                ", trackingId='" + trackingId + '\'' +
                '}';
    }
}
