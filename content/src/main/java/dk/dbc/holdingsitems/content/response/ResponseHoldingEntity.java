package dk.dbc.holdingsitems.content.response;

import dk.dbc.holdingsitems.Record;
import dk.dbc.holdingsitems.RecordCollection;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ResponseHoldingEntity {
    private final String bibliographicRecordId;
    private final String issueId;
    private final String itemId;
    // private final String branchId;
    private final String branch;
    private final String department;
    private final String location;
    private final String subLocation;
    private final String issueText;
    private final String status;
    private final String circulationRule;
    private final int readyForLoan;
    private final String note;

    public ResponseHoldingEntity(Record record, RecordCollection collection) {
        this.bibliographicRecordId = collection.getBibliographicRecordId();
        this.issueId = collection.getIssueId();
        this.itemId = record.getItemId();
        this.branch = record.getBranch();
        this.department = record.getDepartment();
        this.location = record.getLocation();
        this.subLocation = record.getSubLocation();
        this.issueText = collection.getIssueText();
        this.status = record.getStatus();
        this.circulationRule = record.getCirculationRule();
        this.readyForLoan = collection.getReadyForLoan();
        this.note = collection.getNote();
    }

    public String getNote() {
        return note;
    }

    public int getReadyForLoan() {
        return readyForLoan;
    }

    public String getCirculationRule() {
        return circulationRule;
    }

    public String getStatus() {
        return status;
    }

    public String getIssueText() {
        return issueText;
    }

    public String getSubLocation() {
        return subLocation;
    }

    public String getLocation() {
        return location;
    }

    public String getDepartment() {
        return department;
    }

    public String getItemId() {
        return itemId;
    }

    public String getIssueId() {
        return issueId;
    }

    public String getBibliographicRecordId() {
        return bibliographicRecordId;
    }

    public String getBranch() {
        return branch;
    }

    public static List<ResponseHoldingEntity> listFromRecordCollection(RecordCollection rc) {
        final Iterable<Record> recordIterable = () -> rc.iterator();
        final Stream<Record> recordList = StreamSupport.stream(recordIterable.spliterator(), false);
        final List<ResponseHoldingEntity> holdingEntities = recordList.map(r -> new ResponseHoldingEntity(r, rc)).collect(Collectors.toList());
        return holdingEntities;
    }

}
