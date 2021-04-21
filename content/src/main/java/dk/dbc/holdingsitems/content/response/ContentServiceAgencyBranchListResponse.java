package dk.dbc.holdingsitems.content.response;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Map;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class ContentServiceAgencyBranchListResponse {
    public String trackingId;
    public Map<String, Iterable<String>> holdings;

    public ContentServiceAgencyBranchListResponse(String trackingId, Map<String, Iterable<String>> holdings) {
        this.trackingId = trackingId;
        this.holdings = holdings;
    }
}
