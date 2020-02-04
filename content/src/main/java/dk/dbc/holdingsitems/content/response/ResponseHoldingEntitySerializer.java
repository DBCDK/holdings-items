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
        jsonGenerator.writeStringField("bibliographicRecordId", responseHoldingEntity.getBibliographicRecordId());
        jsonGenerator.writeStringField("issueId", responseHoldingEntity.getIssueId());
        jsonGenerator.writeStringField("itemId", responseHoldingEntity.getItemId());
        // jsonGenerator.writeStringField("branchId", "");
        jsonGenerator.writeStringField("branch", responseHoldingEntity.getBranch());
        jsonGenerator.writeStringField("department", responseHoldingEntity.getDepartment());
        jsonGenerator.writeStringField("location", responseHoldingEntity.getLocation());
        jsonGenerator.writeStringField("subLocation", responseHoldingEntity.getSubLocation());
        jsonGenerator.writeStringField("issueText", responseHoldingEntity.getIssueText());
        jsonGenerator.writeStringField("status", responseHoldingEntity.getStatus().toString());
        jsonGenerator.writeStringField("circulationRule", responseHoldingEntity.getCirculationRule());
        jsonGenerator.writeNumberField("readyForLoan", responseHoldingEntity.getReadyForLoan());
        jsonGenerator.writeStringField("note", responseHoldingEntity.getNote());
        jsonGenerator.writeEndObject();
    }
}