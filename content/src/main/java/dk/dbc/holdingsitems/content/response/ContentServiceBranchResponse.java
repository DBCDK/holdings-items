package dk.dbc.holdingsitems.content.response;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class ContentServiceBranchResponse {
    public String trackingId;
    public  List<CompleteItemFull> completeItems;

    public ContentServiceBranchResponse(String trackingId, List<CompleteItemFull> completeItems ) {
        this.trackingId = ContentServiceItemResponse.generateTrackingIdIfNullOrEmpty(trackingId);
        this.completeItems = completeItems;
    }
}
