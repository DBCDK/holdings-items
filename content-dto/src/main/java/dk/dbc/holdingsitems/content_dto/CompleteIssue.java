/*
 * Copyright (C) 2021 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items-content-service
 *
 * holdings-items-content-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items-content-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.content_dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@SuppressFBWarnings({"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD"})
public class CompleteIssue {

    public String issueId;
    public String issueText;
    public String expectedDelivery;
    public int readyForLoan;
    public List<CompleteItem> items;

    public CompleteIssue() {
    }

    public void merge(CompleteIssue other) {
        if (expectedDelivery == null) {
            expectedDelivery = other.expectedDelivery;
        }
        readyForLoan += other.readyForLoan;
        HashMap<String, CompleteItem> m = items.stream()
                .collect(Collectors.toMap(c -> c.itemId, c -> c, (a, b) -> a, () -> new HashMap<String, CompleteItem>()));

        other.items.forEach(item -> {
            m.putIfAbsent(item.itemId, item);
        });
        items = m.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "CompleteIssue{" + "issueId=" + issueId + ", issueText=" + issueText + ", expectedDelivery=" + expectedDelivery + ", readyForLoan=" + readyForLoan + ", items=" + items + '}';
    }
}
