package dk.dbc.holdingsitems.content.response;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dk.dbc.holdingsitems.Record;
import dk.dbc.holdingsitems.RecordCollection;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ContentServiceItemResponseSerializer  extends StdSerializer<ContentServiceItemResponse> {

    protected ContentServiceItemResponseSerializer(Class<ContentServiceItemResponse> t) {
        super(t);
    }

    @Override
    public void serialize(ContentServiceItemResponse contentServiceItemResponse, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        final RecordCollection rc = contentServiceItemResponse.getRecordCollection();
        final List<ResponseHoldingEntity> holdingEntities = ResponseHoldingEntity.listFromRecordCollection(rc);

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("trackingId", contentServiceItemResponse.getTrackingId());
        jsonGenerator.writeObjectField("holdings", holdingEntities);
        jsonGenerator.writeEndObject();
    }
}