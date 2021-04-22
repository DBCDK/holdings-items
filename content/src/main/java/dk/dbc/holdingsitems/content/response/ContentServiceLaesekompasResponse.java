package dk.dbc.holdingsitems.content.response;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Map;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class ContentServiceLaesekompasResponse {
    public String trackingId;
    public Map<String, Iterable<LaesekompasHoldingsEntity>> holdings;

    public ContentServiceLaesekompasResponse(String trackingId, Map<String, Iterable<LaesekompasHoldingsEntity>> holdings) {
        this.trackingId = trackingId;
        this.holdings = holdings;
    }
}
