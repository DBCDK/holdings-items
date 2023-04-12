package dk.dbc.holdingsitems.content;

import com.github.dockerjava.api.model.ContainerNetwork;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import java.net.URI;
import java.time.Duration;
import javax.ws.rs.core.UriBuilder;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class AbstractITBase extends JpaBase {

    private static final GenericContainer PAYARA = makeService(PG);
    static final UriBuilder API_URI = UriBuilder.fromUri(URI.create("http://" + containerIp(PAYARA) + ":8080")).path("/api");

    private static GenericContainer makeService(DBCPostgreSQLContainer pg) {
        String dockerImagePostfix = System.getProperty("docker.image.postfix", "-current:latest");
        GenericContainer container = new GenericContainer("holdings-items-content-service" + dockerImagePostfix)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("dk.dbc.PAYARA")))
                .withEnv("JAVA_MAX_HEAP_SIZE", "1G")
                .withEnv("HOLDINGS_ITEMS_POSTGRES_URL", pg.getPayaraDockerJdbcUrl())
                .withEnv("COREPO_SOLR_URL", "zk://not-configured/nowhere")
                .withEnv("LOG__dk_dbc", "DEBUG")
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/health"))
                .withStartupTimeout(Duration.ofMinutes(3));
        container.start();
        return container;
    }

    private static String containerIp(GenericContainer container) {
        return container.getCurrentContainerInfo()
                .getNetworkSettings()
                .getNetworks()
                .values()
                .stream()
                .map(ContainerNetwork::getIpAddress)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Container has no IP address?"));
    }
}
