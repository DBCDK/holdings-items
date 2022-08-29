package dk.dbc.holdingsitems.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class JacksonObjectMapperProvider implements ContextResolver<ObjectMapper> {

    private static final ObjectMapper O = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public ObjectMapper getContext(Class<?> type) {
        return O;
    }
}
