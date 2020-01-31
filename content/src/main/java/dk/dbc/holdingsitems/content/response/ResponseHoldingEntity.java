package dk.dbc.holdingsitems.content.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import dk.dbc.holdingsitems.jpa.ItemEntity;

@JsonSerialize(using = ResponseHoldingEntitySerializer.class)
public class ResponseHoldingEntity {

    private ItemEntity item;

    public ResponseHoldingEntity(ItemEntity item) {
        this.item = item;
    }

    public ItemEntity getItem() {
        return item;
    }
}