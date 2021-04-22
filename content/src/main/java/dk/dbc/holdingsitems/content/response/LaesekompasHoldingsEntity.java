package dk.dbc.holdingsitems.content.response;

import dk.dbc.holdingsitems.jpa.Status;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class LaesekompasHoldingsEntity {
    public int agencyId;
    public String bibliographicRecordId;
    public String branch;
    public Status status;

    public LaesekompasHoldingsEntity(int agencyId, String bibliographicRecordId, String branch, Status status) {
        this.agencyId = agencyId;
        this.bibliographicRecordId = bibliographicRecordId;
        this.branch = branch;
        this.status = status;
    }

    public static LaesekompasHoldingsEntity fromDatabaseObjects(Object[] databaseStrings) {
        if (databaseStrings.length != 4) {
            return null;
        }
        return new LaesekompasHoldingsEntity(
                (int) databaseStrings[0],
                (String) databaseStrings[1],
                (String) databaseStrings[2],
                (Status) databaseStrings[3]
        );
    }
}
