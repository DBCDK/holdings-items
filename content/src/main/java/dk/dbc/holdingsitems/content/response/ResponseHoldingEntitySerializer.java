package dk.dbc.holdingsitems.content.response;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class ResponseHoldingEntitySerializer extends StdSerializer<ResponseHoldingEntity> {

    public ResponseHoldingEntitySerializer() {
        this(null);
    }

    protected ResponseHoldingEntitySerializer(Class<ResponseHoldingEntity> t) {
        super(t);
    }

    @Override
    public void serialize(ResponseHoldingEntity responseHoldingEntity, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("bibliographicRecordId", responseHoldingEntity.getItem().getBibliographicRecordId());
        jsonGenerator.writeStringField("issueId", responseHoldingEntity.getItem().getIssueId());
        jsonGenerator.writeStringField("itemId", responseHoldingEntity.getItem().getItemId());
        // jsonGenerator.writeStringField("branchId", "");
        jsonGenerator.writeStringField("branch", responseHoldingEntity.getItem().getBranch());
        jsonGenerator.writeStringField("department", responseHoldingEntity.getItem().getDepartment());
        jsonGenerator.writeStringField("location", responseHoldingEntity.getItem().getLocation());
        jsonGenerator.writeStringField("subLocation", responseHoldingEntity.getItem().getSubLocation());
//        jsonGenerator.writeStringField("issueText", responseHoldingEntity.getItem().getIssueText());
        jsonGenerator.writeStringField("status", responseHoldingEntity.getItem().getStatus().toString());
        jsonGenerator.writeStringField("circulationRule", responseHoldingEntity.getItem().getCirculationRule());
//        jsonGenerator.writeNumberField("readyForLoan", responseHoldingEntity.getItem().getIssueReadyForLoan());
//        jsonGenerator.writeStringField("note", responseHoldingEntity.getItem().getBibliographicItemNote());
        jsonGenerator.writeEndObject();
    }
}