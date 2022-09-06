package dk.dbc.holdingsitems.jpa;

import java.util.Collection;
import java.util.List;

public class TablesWithMigratedContent {

    /**
     * Which tables gets their content fro FlyWay
     *
     * @return collection of tablenames
     */
    public static Collection<String> tablesWithMigratedContent() {
        return List.of("schema_version", "queue_version", "holdingsitems_status", "item_loanrestriction");
    }
}
