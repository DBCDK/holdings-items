package dk.dbc.holdingsitems.content.response;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Set;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class AgenciesWithHoldingsResponse {

    public Set<Integer> agencies;
    public String trackingId;

    public AgenciesWithHoldingsResponse() {
    }

    public AgenciesWithHoldingsResponse(Set<Integer> agencies, String trackingId) {
        this.agencies = agencies;
        this.trackingId = trackingId;
    }
}
