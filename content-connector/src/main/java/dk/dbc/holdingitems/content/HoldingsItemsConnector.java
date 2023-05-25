/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */
package dk.dbc.holdingitems.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpGet;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.eclipse.microprofile.metrics.MetricRegistry;

import static jakarta.ws.rs.core.Response.Status;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * EJB to provide access to the HoldingsItems database.
 */
@Stateless
public class HoldingsItemsConnector {

    public static final Tag METHOD_TAG = new Tag("method", "getAgenciesThatHasHoldingsForId");
    private static final Set<Integer> NO_RETRY_RESPONSES = Set.of(INTERNAL_SERVER_ERROR.getStatusCode(), BAD_REQUEST.getStatusCode());
    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response -> NO_RETRY_RESPONSES.contains(response.getStatus()))
            .withDelay(Duration.ofSeconds(1))
            .withMaxRetries(2);

    @Inject
    MetricRegistry mr;

    @Inject
    @ConfigProperty(name = "HOLDING_ITEMS_CONTENT_SERVICE_URL")
    private String holdingsServiceUrl;

    @Inject
    @ConfigProperty(name = "HOLDINGS_ITEMS_CONNECT_TIMEOUT", defaultValue = "PT1S")
    private Duration connectTimeout;

    @Inject
    @ConfigProperty(name = "HOLDINGS_ITEMS_READ_TIMEOUT", defaultValue = "PT1S")
    private Duration readTimeout;

    private HttpClient httpClient;

    private Consumer<Tag[]> holdingsItemsErrorCounterMetrics;

    private BiConsumer<Duration, Tag[]> holdingsItemsTimingMetrics;

    private static Tag[] tags(Tag... tags) {
        return tags;
    }

    protected static final String ERROR_TYPE = "errortype";

    @SuppressWarnings("unused")
    public HoldingsItemsConnector() {
    }

    public HoldingsItemsConnector(MetricRegistry mr, String holdingsServiceUrl) {
        this.mr = mr;
        this.holdingsServiceUrl = holdingsServiceUrl;
        connectTimeout = Duration.ofSeconds(1);
        readTimeout = Duration.ofSeconds(1);
        init();
    }

    @PostConstruct
    public void init() {
        Client client = ClientBuilder.newBuilder()
                .connectTimeout(connectTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .build();
        httpClient = FailSafeHttpClient.create(client, RETRY_POLICY);
        if (mr != null) {
            Metadata counterMetadata = Metadata.builder()
                    .withName("update_holdingsitems_error_counter")
                    .withDescription("Number of errors caught in various holdingsitems calls")
                    .withType(MetricType.COUNTER)
                    .withUnit("requests")
                    .build();
            holdingsItemsErrorCounterMetrics = (tags) -> mr.counter(counterMetadata, tags).inc();

            Metadata timerMetadata = Metadata.builder()
                    .withName("update_holdingsitems_timer")
                    .withDescription("Duration of various various holdingsitems calls")
                    .withUnit(MetricUnits.MILLISECONDS)
                    .withType(MetricType.SIMPLE_TIMER)
                    .build();
            holdingsItemsTimingMetrics = (duration, tags) -> mr.timer(timerMetadata, tags).update(duration);
        } else {
            holdingsItemsErrorCounterMetrics = (tag) -> {
            };
            holdingsItemsTimingMetrics = (duration, tags) -> {
            };
        }
    }

    public Set<String> getHoldings(int agencyId) {
        try (Response response = new HttpGet(httpClient).withBaseUrl(holdingsServiceUrl + "/holdings-by-agency-id/" + agencyId).execute()) {
            if (response.getStatusInfo().toEnum() == Status.NOT_FOUND) {
                return Set.of();
            } else if (response.getStatus() >= 400) {
                throw new InternalServerErrorException("Failed to fetch holdings for agency : " + agencyId + ", reason: " + response.getStatusInfo().toEnum());
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.readEntity(InputStream.class), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.toSet());
            } catch (IOException e) {
                throw new ProcessingException("Failed to fetch holdings for agency " + agencyId, e);
            }
        }
    }

    public Set<Integer> getAgenciesWithHoldings(String id) {
        Instant start = Instant.now();
        try (Response response = new HttpGet(httpClient).withBaseUrl(holdingsServiceUrl + "/agencies-with-holdings/" + id).execute()) {
            if (response.getStatusInfo().toEnum() == Status.NOT_FOUND) {
                return Set.of();
            } else if (response.getStatus() >= 400) {
                throw new InternalServerErrorException("Failed to fetch agencies for record : " + id + ", reason: " + response.getStatusInfo().toEnum());
            }
            HoldingsResponse holdingsResponse = response.readEntity(HoldingsResponse.class);
            return holdingsResponse.agencies;
        } catch (RuntimeException e) {
            holdingsItemsErrorCounterMetrics.accept(tags(METHOD_TAG, new Tag(ERROR_TYPE, e.getMessage().toLowerCase())));
            throw e;
        } finally {
            holdingsItemsTimingMetrics.accept(Duration.between(start, Instant.now()), tags(METHOD_TAG));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HoldingsResponse {

        private Set<Integer> agencies;
        private String trackingId;

        public Set<Integer> getAgencies() {
            return agencies;
        }

        public void setAgencies(Set<Integer> agencies) {
            this.agencies = agencies;
        }

        public String getTrackingId() {
            return trackingId;
        }

        public void setTrackingId(String trackingId) {
            this.trackingId = trackingId;
        }
    }
}
