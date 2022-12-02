package dk.dbc.holdingsitems.jpa;

import javax.persistence.criteria.CriteriaBuilder;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/** Class to represent the resulting table when asking for an agencys total count holdings-items grouped by status
 * Not 100% sure this class is needed or if it isn't just unnecessary overhead ...
 */

public class StatusCountEntity implements Serializable {

    // same as in all other entity classes
    private static final long serialVersionUID = 1089023457634768914L;

    private final String trackingId;
    private final int agencyId;
    private final Map<String, Long> statusCounts;

    public StatusCountEntity(int agency, String trackingId) {
        this.trackingId = trackingId;
        this.agencyId = agency;
        statusCounts = new HashMap<>();
    }

    public String getTrackingId() {
        return trackingId;
    }

    public Integer getAgencyId() {
        return agencyId;
    }

    public Map<String, Long> getStatusCounts() {
        return statusCounts;
    }

    @Override
    public String toString() {
        return "StatusCountEntity{" +
                "statusCounts=" + statusCounts +
                '}';
    }
}
