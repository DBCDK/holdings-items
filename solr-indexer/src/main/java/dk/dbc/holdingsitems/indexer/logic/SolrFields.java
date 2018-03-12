/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of dbc-git:holdings-items-solr-indexer
 *
 * dbc-git:holdings-items-solr-indexer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dbc-git:holdings-items-solr-indexer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.indexer.logic;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public enum SolrFields {

    ID("id"),
    REC_BIBLIOGRAPHIC_RECORD_ID("rec.bibliographicRecordId"),
    BIBLIOGRAPHIC_RECORD_ID("holdingsitem.bibliographicRecordId"),
    AGENCY_ID("holdingsitem.agencyId"),
    ISSUE_ID("holdingsitem.issueId"),
    ISSUE_ID_LITERAL("holdingsitem.issueId.literal"),
    ISSUE_TEXT("holdingsitem.issueText"),
    ITEM_ID("holdingsitem.itemId"),
    BRANCH("holdingsitem.branch"),
    DEPARTMENT("holdingsitem.department"),
    LOCATION("holdingsitem.location"),
    SUBLOCATION("holdingsitem.subLocation"),
    CIRCULATION_RULE("holdingsitem.circulationRule"),
    STATUS("holdingsitem.status"),
    EXPECTED_DELIVERY("holdingsitem.expectedDelivery"),
    READY_FOR_LOAN("holdingsitem.readyForLoan"),
    ACCESSION_DATE("holdingsitem.accessionDate"),
    PARENT_DOCUMENT("parentDocId"),
    NOTE("holdingsitem.note"),
    COLLECTION_ID("holdingsitem.collectionId"),
    TRACKING_ID("rec.trackingId"),
    INDEXED_DATE("rec.indexedDate");


    private final String fieldName;

    private SolrFields(final String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * @return the fieldName
     */
    public String getFieldName() {
        return fieldName;
    }
}