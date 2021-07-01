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
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Embeddable
public class IssueKey implements Serializable {

    private static final long serialVersionUID = -7890645376134515443L;

    @EmbeddedId
    private BibliographicItemKey collection;

    @Column(updatable = false, insertable = false, nullable = false)
    private String issueId;

    public IssueKey() {
        collection = new BibliographicItemKey();
    }

    public IssueKey(int agencyId, String bibliographicRecordId, String issueId) {
        this.collection = new BibliographicItemKey(agencyId, bibliographicRecordId);
        this.issueId = issueId;
    }

    public int getAgencyId() {
        return collection.getAgencyId();
    }

    public String getBibliographicRecordId() {
        return collection.getBibliographicRecordId();
    }

    public String getIssueId() {
        return issueId;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.collection);
        hash = 67 * hash + Objects.hashCode(this.issueId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final IssueKey other = (IssueKey) obj;
        return Objects.equals(this.collection, other.collection) &&
               Objects.equals(this.issueId, other.issueId);
    }

    @Override
    public String toString() {
        return "IssueKey{" + "agencyId=" + getAgencyId() + ", bibliographicRecordId=" + getBibliographicRecordId() + ", issueId=" + issueId + '}';
    }
}
