package dk.dbc.holdingsitems.content.response;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class ContentServiceItemResponseSerializer  extends StdSerializer<ContentServiceItemResponse> {

    protected ContentServiceItemResponseSerializer(Class<ContentServiceItemResponse> t) {
        super(t);
    }

    @Override
    public void serialize(ContentServiceItemResponse contentServiceItemResponse, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("trackingId", contentServiceItemResponse.getTrackingId());
        jsonGenerator.writeObjectField("holdings", contentServiceItemResponse.getHoldings());
        jsonGenerator.writeEndObject();
    }
}