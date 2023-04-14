package dk.dbc.holdingsitems.content.solr;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static dk.dbc.holdingsitems.content.solr.SolrFields.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class SolrPidResponse {

    public String trackingId;
    public Map<String, Map<String, String>> holdings;

    public SolrPidResponse(String trackingId) {
        this.trackingId = trackingId;
        this.holdings = new HashMap<>();
    }

    public Consumer<SolrStreamedDoc> consumerFor(String pid) {
        Map<String, String> items = holdings.computeIfAbsent(pid, p -> new HashMap<>());
        return (doc) -> {
            String status = doc.getValue(HOLDINGSITEM_STATUS_FOR_STREAMING);
            doc.getValues(HOLDINGSITEM_ITEM_ID).forEach(itemId -> {
                items.put(itemId, status);
            });
        };
    }
}
