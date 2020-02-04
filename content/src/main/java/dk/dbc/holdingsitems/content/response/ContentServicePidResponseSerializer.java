package dk.dbc.holdingsitems.content.response;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dk.dbc.holdingsitems.RecordCollection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ContentServicePidResponseSerializer extends StdSerializer<ContentServicePidResponse> {

    protected ContentServicePidResponseSerializer(Class<ContentServicePidResponse> t) {
        super(t);
    }

    @Override
    public void serialize(ContentServicePidResponse contentServicePidResponse, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        Map<String, List<ResponseHoldingEntity>> responseMap = new HashMap<>();
        Iterator<Map.Entry<String, RecordCollection>> itr = contentServicePidResponse.getHoldingsMap().entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, RecordCollection> entry = itr.next();
            responseMap.put(entry.getKey(), ResponseHoldingEntity.listFromRecordCollection(entry.getValue()));
        }
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("trackingId", contentServicePidResponse.getTrackingId());
        jsonGenerator.writeObjectField("holdings", responseMap);
        jsonGenerator.writeEndObject();
    }
}