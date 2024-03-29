/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items
 *
 * holdings-items is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.indexer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJBException;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Singleton
@Startup
@Lock(LockType.READ)
public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    public static final String DATABASE = "jdbc/holdings-items";
    public static final String PROPERTIES = "holdings-items-indexer";

    private Properties props;
    private String[] queues;
    private int threads;
    private String solrDocStoreUrl;
    private Client client;
    private String holdingsItemsContentServiceUrl;

    public Config() {
        props = null;
    }

    Config(String... props) {
        this.props = new Properties();
        for (String prop : props) {
            String[] parts = prop.split("=", 2);
            this.props.setProperty(parts[0], parts[1]);
        }
    }

    @PostConstruct
    public void init() {
        log.info("Setting up config");
        if (props == null) {
            props = findProperties(PROPERTIES);
        }
        this.queues = Arrays.stream(getOrFail("queues").split("[\\s,]+"))
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        this.threads = Integer.max(1, Integer.parseInt(getOrDefault("threads", "5")));
        this.solrDocStoreUrl = URI.create(getOrFail("solr-doc-store-url"))
                .resolve("/api/holdings")
                .toString();
        this.holdingsItemsContentServiceUrl = URI.create(getOrFail("holdings-items-content-service-url"))
                .resolve("/api/complete")
                .toString();

        String userAgent = getOrDefault("user-agent", "Unknown/0.0");
        log.debug("Using: {} as HttpUserAgent", userAgent);
        this.client = clientBuilder()
                .register((ClientRequestFilter) (ClientRequestContext context) -> {
                    context.getHeaders().putSingle("User-Agent", userAgent);
                }).build();
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public String[] getQueues() {
        return queues;
    }

    public int getThreads() {
        return threads;
    }

    public String getSolrDocStoreUrl() {
        return solrDocStoreUrl;
    }

    public String getHoldingsItemsContentServiceUrl() {
        return holdingsItemsContentServiceUrl;
    }

    public Client getClient() {
        return client;
    }

    private String getOrFail(String property) {
        String env = property.toUpperCase(Locale.ROOT).replaceAll("[^0-9A-Z_]", "_");
        String ret = props.getProperty(property, System.getenv(env));
        if (ret == null) {
            throw new EJBException("Configuration is needed for " + property + " set " + property + "/" + env);
        }
        return ret;
    }

    private String getOrDefault(String property, String defaultValue) {
        String env = property.toUpperCase(Locale.ROOT).replaceAll("[^0-9A-Z_]", "_");
        String ret = props.getProperty(property, System.getenv(env));
        if (ret == null) {
            ret = defaultValue;
        }
        return ret;
    }

    private Properties findProperties(String resourceName) {
        try {
            Object loopup = InitialContext.doLookup(resourceName);
            if (loopup instanceof Properties) {
                return (Properties) loopup;
            } else {
                throw new NamingException("Found " + resourceName + ", but not of type Properties of type: " + loopup.getClass().getTypeName());
            }
        } catch (NamingException ex) {
            log.info("Exception: {}", ex.getMessage());
        }
        return new Properties();
    }

    protected ClientBuilder clientBuilder() {
        return ClientBuilder.newBuilder();
    }

    @Override
    public String toString() {
        return "Config{queues=" + Arrays.toString(queues) + ", threads=" + threads + ", solrDocStoreUrl=" + solrDocStoreUrl + ", holdingsItemsContentServiceUrl=" + holdingsItemsContentServiceUrl + '}';
    }
}
