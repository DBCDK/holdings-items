package dk.dbc.holdingsitems.content.response;

import dk.dbc.holdingsitems.Record;
import dk.dbc.holdingsitems.RecordCollection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class ResponseHoldingEntity {
    public String bibliographicRecordId;
    public String issueId;
    public String itemId;
    // private final String branchId;
    public String branch;
    public String department;
    public String location;
    public String subLocation;
    public String issueText;
    public String status;
    public String circulationRule;
    public int readyForLoan;
    public String note;

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

    public static List<ResponseHoldingEntity> listFromRecordCollection(RecordCollection rc) {
        if (rc == null) {
            return new ArrayList<>();
        }
        final Iterable<Record> recordIterable = () -> rc.iterator();
        final Stream<Record> recordList = StreamSupport.stream(recordIterable.spliterator(), false);
        final List<ResponseHoldingEntity> holdingEntities = recordList.map(r -> new ResponseHoldingEntity(r, rc)).collect(Collectors.toList());
        return holdingEntities;
    }

}
