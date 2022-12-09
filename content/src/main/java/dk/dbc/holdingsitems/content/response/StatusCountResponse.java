package dk.dbc.holdingsitems.content.response;

import dk.dbc.holdingsitems.jpa.Status;

import java.util.Map;

/**
 * Class to represent object to return to endpoint at status per agency query
 */
public class StatusCountResponse {

    public final Map<Status, Long> statusCounts;
    public final String trackingId;
    public final int agencyId;

    public StatusCountResponse(int agencyId, Map<Status, Long> statusCountsByAgency, String trackingId) {
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
