package dk.dbc.holdingsitems;

import dk.dbc.holdingsitems.jpa.Status;

import java.io.Serializable;
import java.util.Map;

/**
 * Class to represent the resulting table when asking for an agencys total count holdings-items grouped by status
 * Not 100% sure this class is needed or if it isn't just unnecessary overhead ...
 */
public class TotalStatusCountsForAgency implements Serializable {

    // same as in all other entity classes
    private static final long serialVersionUID = 1089023457634768914L;

    private final String trackingId;
    private final int agencyId;
    private final Map<Status, Long> statusCounts;

    public TotalStatusCountsForAgency(int agency, Map<Status, Long> statusCounts, String trackingId) {
        this.trackingId = trackingId;
        this.agencyId = agency;
        this.statusCounts = statusCounts;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public Integer getAgencyId() {
        return agencyId;
    }

    public Map<Status, Long> getStatusCounts() {
        return statusCounts;
    }

    @Override
    public String toString() {
        return "TotalStatusCountsForAgency{" +
                "statusCounts=" + statusCounts +
                "}";
    }
}
