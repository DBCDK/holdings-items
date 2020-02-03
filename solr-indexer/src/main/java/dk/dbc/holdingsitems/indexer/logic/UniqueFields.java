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

import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import java.time.LocalDate;
import java.util.Objects;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class UniqueFields {

    private final int agencyId;
    private final String bibliographicRecordId;
    private final String issueId;
    private final String issueText;
    private final LocalDate expectedDelivery;
    private final int readyForLoan;
    private final String branch;
    private final String branchId;
    private final String department;
    private final String location;
    private final String subLocation;
    private final String circulationRule;
    private final LocalDate accessionDate;
    private final String loanRestriction;

    public UniqueFields(IssueEntity collection, ItemEntity record) {
        this.agencyId = collection.getAgencyId();
        this.bibliographicRecordId = collection.getBibliographicRecordId();
        this.issueId = collection.getIssueId();
        this.issueText = collection.getIssueText();
        this.expectedDelivery = collection.getExpectedDelivery();
        this.readyForLoan = collection.getReadyForLoan();
        this.branch = record.getBranch();
        this.branchId = record.getBranchId();
        this.department = record.getDepartment();
        this.location = record.getLocation();
        this.subLocation = record.getSubLocation();
        this.circulationRule = record.getCirculationRule();
        this.accessionDate = record.getAccessionDate();
        this.loanRestriction = record.getLoanRestriction();
    }

    public void fillIn(ObjectNode node) {
        node.putArray(SolrFields.AGENCY_ID.getFieldName()).add(String.valueOf(agencyId));
        node.putArray(SolrFields.BIBLIOGRAPHIC_RECORD_ID.getFieldName()).add(bibliographicRecordId);
        node.putArray(SolrFields.REC_BIBLIOGRAPHIC_RECORD_ID.getFieldName()).add(bibliographicRecordId);
        node.putArray(SolrFields.ISSUE_ID.getFieldName()).add(issueId);
        node.putArray(SolrFields.ISSUE_TEXT.getFieldName()).add(issueText);
        if (expectedDelivery != null)
            node.putArray(SolrFields.EXPECTED_DELIVERY.getFieldName()).add(expectedDelivery.toString());
        node.putArray(SolrFields.READY_FOR_LOAN.getFieldName()).add(String.valueOf(readyForLoan));
        node.putArray(SolrFields.BRANCH.getFieldName()).add(branch);
        node.putArray(SolrFields.BRANCH_ID.getFieldName()).add(String.valueOf(branchId));
        node.putArray(SolrFields.DEPARTMENT.getFieldName()).add(department);
        node.putArray(SolrFields.LOCATION.getFieldName()).add(location);
        node.putArray(SolrFields.SUBLOCATION.getFieldName()).add(subLocation);
        node.putArray(SolrFields.CIRCULATION_RULE.getFieldName()).add(circulationRule);
        node.putArray(SolrFields.ACCESSION_DATE.getFieldName()).add(String.valueOf(accessionDate) + "T00:00:00.000Z");
        if (!loanRestriction.isEmpty())
            node.putArray(SolrFields.LOAN_RESTRICTION.getFieldName()).add(loanRestriction);
        node.putArray(SolrFields.COLLECTION_ID.getFieldName()).add(String.valueOf(agencyId) + "-" + bibliographicRecordId.replaceAll("[^0-9a-zA-Z]", "_"));
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + this.agencyId;
        hash = 43 * hash + Objects.hashCode(this.bibliographicRecordId);
        hash = 43 * hash + Objects.hashCode(this.issueId);
        hash = 43 * hash + Objects.hashCode(this.issueText);
        hash = 43 * hash + Objects.hashCode(this.expectedDelivery);
        hash = 43 * hash + this.readyForLoan;
        hash = 43 * hash + Objects.hashCode(this.branch);
        hash = 43 * hash + Objects.hashCode(this.branchId);
        hash = 43 * hash + Objects.hashCode(this.department);
        hash = 43 * hash + Objects.hashCode(this.location);
        hash = 43 * hash + Objects.hashCode(this.subLocation);
        hash = 43 * hash + Objects.hashCode(this.circulationRule);
        hash = 43 * hash + Objects.hashCode(this.accessionDate);
        hash = 43 * hash + Objects.hashCode(this.loanRestriction);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final UniqueFields other = (UniqueFields) obj;
        return this.agencyId == other.agencyId &&
               this.readyForLoan == other.readyForLoan &&
               Objects.equals(this.bibliographicRecordId, other.bibliographicRecordId) &&
               Objects.equals(this.issueId, other.issueId) &&
               Objects.equals(this.issueText, other.issueText) &&
               Objects.equals(this.branch, other.branch) &&
               Objects.equals(this.branchId, other.branchId) &&
               Objects.equals(this.department, other.department) &&
               Objects.equals(this.location, other.location) &&
               Objects.equals(this.subLocation, other.subLocation) &&
               Objects.equals(this.circulationRule, other.circulationRule) &&
               Objects.equals(this.expectedDelivery, other.expectedDelivery) &&
               Objects.equals(this.accessionDate, other.accessionDate) &&
               Objects.equals(this.loanRestriction, other.loanRestriction);
    }

    @Override
    public String toString() {
        return "UniqueFields{" + "agencyId=" + agencyId + ", bibliographicRecordId=" + bibliographicRecordId + ", issueId=" + issueId + ", issueText=" + issueText + ", expectedDelivery=" + expectedDelivery + ", readyForLoan=" + readyForLoan + ", branch=" + branch + ", branchId=" + branchId + ", department=" + department + ", location=" + location + ", subLocation=" + subLocation + ", circulationRule=" + circulationRule + ", accessionDate=" + accessionDate + ", loanRestriction=" + loanRestriction + '}';
    }

}
