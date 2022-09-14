package dk.dbc.holdingsitems.indexer.logic;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.function.Predicate.not;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ItemGrouping {

    private static final Logger log = LoggerFactory.getLogger(ItemGrouping.class);

    private static final Set<String> REPEATABLE_FIELDS = Set.of(
            SolrFields.BIBLIOGRAPHIC_RECORD_ID.getFieldName(),
            SolrFields.BRANCH.getFieldName(),
            SolrFields.ITEM_ID.getFieldName(),
            SolrFields.TRACKING_ID.getFieldName());

    private final Map<Map<String, Set<String>>, Map<String, Set<String>>> map;
    private int counter = 0;

    public ItemGrouping() {
        this.map = new HashMap<>();
    }

    public void add(ItemMerge item) {
        Map<String, Set<String>> key = item.mapOf(not(REPEATABLE_FIELDS::contains));
        // When created all non-repeateble fields are duplicated into the value
        Map<String, Set<String>> repeatedFields = map.computeIfAbsent(key, HashMap<String, Set<String>>::new);
        // All repeatable fields are merged into the non-repeatable (and previously merged repeatable)
        Map<String, Set<String>> value = item.mapOf(REPEATABLE_FIELDS::contains);
        value.forEach((k, v) -> repeatedFields.computeIfAbsent(k, x -> new HashSet<>()).addAll(v));
        counter++;
    }

    public Collection<Map<String, Set<String>>> documents() {
        Collection<Map<String, Set<String>>> values = map.values();
        log.debug("Compacted SolR documents {} -> {}", counter, values.size());
        return values;
    }
}
