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
import dk.dbc.holdingsitems.jpa.ItemEntity;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class RepeatedFields {

    private final Set<String> branches;
    private final Set<String> itemIds;
    private final Set<String> trackingIds;

    public RepeatedFields() {
        this.branches = new HashSet<>();
        this.itemIds = new HashSet<>();
        this.trackingIds = new HashSet<>();
    }

    public RepeatedFields(String... trackingIds) {
        this();
        this.trackingIds.addAll(Arrays.asList(trackingIds));
    }

    public void addRepeatedFieldsFrom(ItemEntity item) {
        addBranch(item.getBranch());
        addItemId(item.getItemId());
        addTrackingId(item.getTrackingId());
    }

    private void addBranch(String branch) {
        branches.add(branch);
    }

    private void addItemId(String itemId) {
        itemIds.add(itemId);
    }

    private void addTrackingId(String trackingId) {
        if (trackingId != null && !trackingId.isEmpty()) {
            trackingIds.add(trackingId);
        }
    }

    public void fillIn(ObjectNode node) {
        addAll(node, SolrFields.BRANCH, branches);
        addAll(node, SolrFields.ITEM_ID, itemIds);
        addAll(node, SolrFields.TRACKING_ID, trackingIds);
    }

    private void addAll(ObjectNode node, SolrFields field, Collection<String> values) {
        ArrayNode array = node.putArray(field.getFieldName());
        values.forEach(array::add);
    }

    @Override
    public String toString() {
        return "RepeatedFields{" + "branchs=" + branches + ", itemIds=" + itemIds + ", trackingIds=" + trackingIds + '}';
    }

}
