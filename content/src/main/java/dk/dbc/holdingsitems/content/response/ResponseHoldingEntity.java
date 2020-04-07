package dk.dbc.holdingsitems.content.response;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
    public String LoanRestriction;

}
