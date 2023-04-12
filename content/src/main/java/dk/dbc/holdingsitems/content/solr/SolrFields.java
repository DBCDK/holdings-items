package dk.dbc.holdingsitems.content.solr;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public enum SolrFields {

    HOLDINGSITEM_ID("id"),
    HOLDINGSITEM_BIBLIOGRAPHIC_RECORD_ID("holdingsitem.bibliographicRecordId"),
    HOLDINGSITEM_AGENCY_ID("holdingsitem.agencyId"),
    HOLDINGSITEM_BIBLIOGRAPHIC_OWNER_AGENCY_ID("holdingsitem.bibliographicOwnerAgencyId"),
    HOLDINGSITEM_ISSUE_ID("holdingsitem.issueId"),
    HOLDINGSITEM_ISSUE_TEXT("holdingsitem.issueText"),
    HOLDINGSITEM_ITEM_ID("holdingsitem.itemId"),
    HOLDINGSITEM_BRANCH_ID("holdingsitem.branchId"),
    HOLDINGSITEM_BRANCH("holdingsitem.branch"),
    HOLDINGSITEM_DEPARTMENT("holdingsitem.department"),
    HOLDINGSITEM_LOCATION("holdingsitem.location"),
    HOLDINGSITEM_SUBLOCATION("holdingsitem.subLocation"),
    HOLDINGSITEM_CIRCULATION_RULE("holdingsitem.circulationRule"),
    HOLDINGSITEM_STATUS("holdingsitem.status"),
    HOLDINGSITEM_STATUS_FOR_STREAMING("holdingsitem_dv.status"),
    HOLDINGSITEM_EXPECTED_DELIVERY("holdingsitem.expectedDelivery"),
    HOLDINGSITEM_READY_FOR_LOAN("holdingsitem.readyForLoan"),
    HOLDINGSITEM_ACCESSION_DATE("holdingsitem.accessionDate"),
    HOLDINGSITEM_NOTE("holdingsitem.note"),
    HOLDINGSITEM_ROLE("holdingsitem.role"),
    HOLDINGSITEM_CALL_NUMBER("holdingsitem.callNumber"),
    HOLDINGSITEM_LOAN_RESTRICTION("holdingsitem.loanRestriction"),
    REC_BIBLIOGRAPHIC_RECORD_ID("rec.bibliographicRecordId"),
    REC_UNIT_ID("rec.unitId"),
    REC_TRACKING_ID("rec.trackingId"),
    REC_REPOSITORY_ID("rec.repositoryId"),
    REC_MANIFESTATION_ID("rec.manifestationId"),
    REC_EXCLUDE_FROM_UNION_CATALOGUE("rec.excludeFromUnionCatalogue"),
    REC_INDEXED_DATE("rec.indexedDate");

    private final String fieldName;

    SolrFields(final String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * @return the fieldName
     */
    public String getFieldName() {
        return fieldName;
    }
}
