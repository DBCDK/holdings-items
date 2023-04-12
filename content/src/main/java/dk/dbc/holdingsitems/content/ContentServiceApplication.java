package dk.dbc.holdingsitems.content;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.jackson.JacksonFeature;

import static java.util.stream.Collectors.toSet;

@ApplicationPath("api")
public class ContentServiceApplication extends Application {

    public static final String API_VERSION = "v1";

    private static final Set<Class<?>> CLASSES = makeClassesSet();

    private static Set<Class<?>> makeClassesSet() {
        return Stream.of(// api/v1
                ContentResource.class,
                Solr.class,
                Status.class,
                Supersedes.class,
                // developer
                DeveloperResource.class,
                // common
                JacksonFeature.class,
                JacksonObjectMapperProvider.class)
                .collect(toSet());
    }

    @Override
    @SuppressFBWarnings(value = "EI_EXPOSE_REP")
    public Set<Class<?>> getClasses() {
        return CLASSES;
    }

    @Override
    public Set<Object> getSingletons() {
        return new HashSet<>(super.getSingletons());
    }
}
