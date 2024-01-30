package dk.dbc.holdingsitems.content.solr;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.HashSet;

import static dk.dbc.holdingsitems.content.solr.SolrFields.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class SolrHoldingsResponse {

    private final String trackingId;
    private String lastBiblId;

    private final HashSet<String> seenStatusForThisBibId = new HashSet<>();
    private final HashMap<String, Integer> bibWithStatus = new HashMap<>();
    private final HashMap<String, Integer> itemWithStatus = new HashMap<>();

    public SolrHoldingsResponse(String trackingId) {
        this.trackingId = trackingId;
    }

    public void consume(SolrStreamedDoc doc) {
        String bibId = doc.getValue(HOLDINGSITEM_BIBLIOGRAPHIC_RECORD_ID);
        if (!bibId.equals(lastBiblId)) {
            seenStatusForThisBibId.clear();
            lastBiblId = bibId;
        }
        String status = doc.getValue(HOLDINGSITEM_STATUS_FOR_STREAMING);
        if (seenStatusForThisBibId.add(status)) {
            bibWithStatus.merge(status, 1, Integer::sum);
        }
        itemWithStatus.merge(status, doc.getValuesOptional(HOLDINGSITEM_ITEM_ID).size(), Integer::sum);
    }

    @JsonProperty("bibliographicItems")
    public HashMap<String, Integer> getBibWithStatus() {
        return bibWithStatus;
    }

    @JsonProperty("items")
    public HashMap<String, Integer> getItemWithStatus() {
        return itemWithStatus;
    }

    public String getTrackingId() {
        return trackingId;
    }

    @Override
    public String toString() {
        return "Accumulator{" + "bibWithStatus=" + bibWithStatus + ", itemWithStatus=" + itemWithStatus + '}';
    }
}
