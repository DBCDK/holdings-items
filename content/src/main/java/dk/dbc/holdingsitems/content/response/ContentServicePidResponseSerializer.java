package dk.dbc.holdingsitems.content.response;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class ContentServicePidResponseSerializer extends StdSerializer<ContentServicePidResponse> {

    protected ContentServicePidResponseSerializer(Class<ContentServicePidResponse> t) {
        super(t);
    }

    @Override
    public void serialize(ContentServicePidResponse contentServicePidResponse, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("trackingId", contentServicePidResponse.getTrackingId());
        jsonGenerator.writeObjectField("holdings", contentServicePidResponse.getHoldingsMap());
        jsonGenerator.writeEndObject();

    }
}