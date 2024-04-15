package dk.dbc.holdingsitems.content.api.v1.update;

import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateStatusEnum;

public class UpdateException extends Exception {

    private static final long serialVersionUID = 0x22D6F596329519F9L;

    private final HoldingsItemsUpdateStatusEnum status;

    public UpdateException(HoldingsItemsUpdateStatusEnum status, String message) {
        super(message);
        this.status = status;
    }

    public HoldingsItemsUpdateStatusEnum getStatus() {
        return status;
    }
}
