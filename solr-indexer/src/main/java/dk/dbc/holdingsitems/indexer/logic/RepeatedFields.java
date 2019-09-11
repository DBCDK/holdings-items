/*
 * Copyright (C) 2017-2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items
 *
 * holdings-items is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.indexer.logic;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dbc.holdingsitems.jpa.HoldingsItemsStatus;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class RepeatedFields {

    private final Set<String> itemIds;
    private final Set<HoldingsItemsStatus> statuses;
    private final Set<String> trackingIds;

    public RepeatedFields() {
        this.itemIds = new HashSet<>();
        this.statuses = new HashSet<>();
        this.trackingIds = new HashSet<>();
    }

    public RepeatedFields(String... trackingIds) {
        this.itemIds = new HashSet<>();
        this.statuses = new HashSet<>();
        this.trackingIds = new HashSet<>();
        this.trackingIds.addAll(Arrays.asList(trackingIds));
    }

    public void addItemId(String itemId) {
        itemIds.add(itemId);
    }

    public void addTrackingId(String trackingId) {
        if (trackingId != null && !trackingId.isEmpty()) {
            trackingIds.add(trackingId);
        }
    }

    public void addStatus(HoldingsItemsStatus status) {
        statuses.add(status);
    }

    public void fillIn(ObjectNode node) {
        addAll(node, SolrFields.ITEM_ID, itemIds);
        addAll(node, SolrFields.STATUS, statuses.stream().map(Object::toString).collect(toList()));
        addAll(node, SolrFields.TRACKING_ID, trackingIds);
    }

    private void addAll(ObjectNode node, SolrFields field, Collection<String> values) {
        ArrayNode array = node.putArray(field.getFieldName());
        values.forEach(array::add);
    }

    @Override
    public String toString() {
        return "RepeatedFields{" + "itemIds=" + itemIds + ", statuses=" + statuses + ", trackingIds=" + trackingIds + '}';
    }

}
