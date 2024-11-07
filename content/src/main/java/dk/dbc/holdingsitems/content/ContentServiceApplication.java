package dk.dbc.holdingsitems.content;

import dk.dbc.commons.payara.helpers.DeveloperModeFilter;
import dk.dbc.commons.payara.helpers.MDCRequestInfo;
import dk.dbc.commons.payara.helpers.RequestLogLevel;
import dk.dbc.holdingsitems.content.api.v1.update.UpdateV1;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Set;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.microprofileext.openapi.swaggerui.OpenApiUiService;

@ApplicationPath("api")
public class ContentServiceApplication extends Application {

    public static final String API_VERSION = "v1";

    private static final Set<Class<?>> CLASSES = DeveloperModeFilter.filter(
            // api/v1
            ContentResource.class,
            Solr.class,
            Status.class,
            Supersedes.class,
            UpdateV1.class,
            // developer
            DeveloperResource.class,
            // common
            MDCRequestInfo.class,
            RequestLogLevel.class,
            JacksonFeature.class,
            JacksonObjectMapperProvider.class,
            OpenApiUiService.class);

    @Override
    @SuppressFBWarnings(value = "EI_EXPOSE_REP")
    public Set<Class<?>> getClasses() {
        return CLASSES;
    }
}
