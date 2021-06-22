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
package dk.dbc.holdingsitems.content.response;

import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class CompleteBibliographic {

    public int agencyId;
    public String bibliographicRecordId;
    public String note;
    public String firstAccessionDate;
    public List<CompleteIssue> issues;
    public String trackingId;

    public CompleteBibliographic() {
    }

    public CompleteBibliographic(BibliographicItemEntity rec, String trackingId) {
        this.agencyId = rec.getAgencyId();
        this.bibliographicRecordId = rec.getBibliographicRecordId();
        this.note = rec.getNote();
        this.firstAccessionDate = rec.getFirstAccessionDate().toString();
        this.issues = rec.stream()
                .sorted((l,r) -> l.getIssueId().compareTo(r.getIssueId()))
                .map(CompleteIssue::new)
                .collect(toList());
        this.trackingId = trackingId;
    }

    @Override
    public String toString() {
        return "CompleteBibliographic{" + "agencyId=" + agencyId + ", bibliographicRecordId=" + bibliographicRecordId + ", note=" + note + ", firstAccessionDate=" + firstAccessionDate + ", issues=" + issues + '}';
    }

}
