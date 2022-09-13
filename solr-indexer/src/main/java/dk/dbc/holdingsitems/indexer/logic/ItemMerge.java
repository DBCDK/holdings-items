package dk.dbc.holdingsitems.indexer.logic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ItemMerge {

    private final Map<String, Set<String>> map;

    private final Map<String, Function<Object, Object>> FIELD_MAPPERS = Map.of(
            SolrFields.LOAN_RESTRICTION.getFieldName(), ItemMerge::loanRestriction,
            SolrFields.NOTE.getFieldName(), ItemMerge::note
    );
    Function<Object, Object> NOOP_FIELD_MAPPER = o -> o;

    public ItemMerge() {
        this.map = new HashMap<>();
    }

    private ItemMerge(HashMap<String, Set<String>> map) {
        this.map = map;
    }

    public ItemMerge with(String key, Object value) {
        Set<String> set = map.computeIfAbsent(key, k -> new HashSet<>());
        value = FIELD_MAPPERS.getOrDefault(key, NOOP_FIELD_MAPPER)
                .apply(value);
        if (value != null) {
            set.add(String.valueOf(value));
        }
        return this;
    }

    public ItemMerge deepCopy() {
        return new ItemMerge(map.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          e -> new HashSet<String>(e.getValue()),
                                          (l, r) -> l,
                                          HashMap<String, Set<String>>::new)));
    }

    public Map<String, Set<String>> mapOf(Predicate<String> predicate) {
        return map.entrySet().stream()
                .filter(e -> predicate.test(e.getKey()))
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public String toString() {
        return "ItemMerge{" + "map=" + map + '}';
    }

    private static Object loanRestriction(Object o) {
        return o == null || "".equals(o) ? null : o;
    }

    private static Object note(Object o) {
        return "".equals(o) ? null : o;
    }
}
