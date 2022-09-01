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
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class CompleteItemFull extends CompleteItem {

    public Integer agencyId;
    public String note;
    public String issueId;
    public String issueText;
    public int readyForLoan;

    public CompleteItemFull() {
    }

    public CompleteItemFull(ItemEntity item) {
        super(item);
        IssueEntity issue = item.getOwner();
        BibliographicItemEntity bibl = issue.getOwner();
        this.agencyId = bibl.getAgencyId();
        this.note = bibl.getNote();
        this.issueId = issue.getIssueId();
        this.issueText = issue.getIssueText();
        this.readyForLoan = issue.getReadyForLoan();
    }

    @Override
    public String toString() {
        return "CompleteItemFull{" + "agencyId=" + agencyId + ", bibliographicRecordId=" + bibliographicRecordId + ", note=" + note + ", issueId=" + issueId + ", issueText=" + issueText + ", readyForLoan=" + readyForLoan + "itemId=" + itemId + ", branchId=" + branchId + ", branch=" + branch + ", department=" + department + ", location=" + location + ", subLocation=" + subLocation + ", circulationRule=" + circulationRule + ", loanRestriction=" + loanRestriction + ", accessionDate=" + accessionDate + ", status=" + status + '}';
    }
}
