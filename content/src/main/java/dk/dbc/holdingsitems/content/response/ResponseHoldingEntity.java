package dk.dbc.holdingsitems.content.response;

import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class ResponseHoldingEntity {
    public String bibliographicRecordId;
    public String issueId;
    public String itemId;
    public String branchId;
    public String branch;
    public String department;
    public String location;
    public String subLocation;
    public String issueText;
    public String status;
    public String circulationRule;
    public int readyForLoan;
    public String note;
    public String loanRestriction;

    public ResponseHoldingEntity(ItemEntity holdingsItem) {
        IssueEntity collection = holdingsItem.getOwner();
        BibliographicItemEntity bibliographicItemEntity = collection.getOwner();
        this.bibliographicRecordId = collection.getBibliographicRecordId();
        this.issueId = collection.getIssueId();
        this.itemId = holdingsItem.getItemId();
        this.branchId = holdingsItem.getBranchId();
        this.branch = holdingsItem.getBranch();
        this.department = holdingsItem.getDepartment();
        this.location = holdingsItem.getLocation();
        this.subLocation = holdingsItem.getSubLocation();
        this.issueText = collection.getIssueText();
        this.status = holdingsItem.getStatus().toString();
        this.circulationRule = holdingsItem.getCirculationRule();
        this.readyForLoan = collection.getReadyForLoan();
        this.note = bibliographicItemEntity.getNote();
        this.loanRestriction = holdingsItem.getLoanRestriction().toString();
    }

    public static List<ResponseHoldingEntity> listFromItems(Iterable<ItemEntity> holdingsItems) {
        return StreamSupport.stream(holdingsItems.spliterator(), false)
                .map(hi -> new ResponseHoldingEntity(hi)).collect(Collectors.toList());
    }

}
