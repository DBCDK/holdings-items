/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items-access
 *
 * holdings-items-access is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items-access is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.jpa;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;

/**
 *
 * Primary key of {@link HoldingsItemsItemEntity}
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Embeddable
public class ItemKey implements Serializable {

    private static final long serialVersionUID = -1235435134573567276L;

    @EmbeddedId
    private final IssueKey collection;

    @Column(updatable = false, insertable = false, nullable = false)
    private String itemId;

    public ItemKey() {
        this.collection = new IssueKey();
    }

    public ItemKey(IssueKey key, String itemId) {
        this.collection = key;
        this.itemId = itemId;
    }

    public ItemKey(int agencyId, String bibliographicRecordId, String issueId, String itemId) {
        this.collection = new IssueKey(agencyId, bibliographicRecordId, issueId);
        this.itemId = itemId;
    }

    public int getAgencyId() {
        return collection.getAgencyId();
    }

    public String getBibliographicRecordId() {
        return collection.getBibliographicRecordId();
    }

    public String getIssueId() {
        return collection.getIssueId();
    }

    public String getItemId() {
        return itemId;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + collection.hashCode();
        hash = 67 * hash + Objects.hashCode(this.itemId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final ItemKey other = (ItemKey) obj;
        return Objects.equals(this.collection, other.collection) &&
               Objects.equals(this.itemId, other.itemId);
    }

}
