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

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.InitialContext;
import javax.naming.NamingException;
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
    private String databaseThrottle;
    private String throttle;
    private String[] queues;
    private long emptyQueueSleep;
    private long maxQueryTime;
    private int idleRescanEvery;
    private int rescanEvery;
    private int threads;
    private int retries;
    private String solrDocStoreUrl;
    private String jmxDomain;

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
        this.databaseThrottle = getOrDefault("database-throttle", "1/5s,3/m,5/10m");
        this.throttle = getOrDefault("throttle", "2/100ms,3/s,5/m");
        this.emptyQueueSleep = Long.max(1000, Long.parseLong(getOrDefault("empty-queue-sleep", "10000")));
        this.maxQueryTime = Long.max(10, Long.parseLong(getOrDefault("max-query-time", "250")));
        this.idleRescanEvery = Integer.max(1, Integer.parseInt(getOrDefault("idle-rescan-every", "10")));
        this.rescanEvery = Integer.max(1, Integer.parseInt(getOrDefault("rescan-every", "1000")));
        this.threads = Integer.max(1, Integer.parseInt(getOrDefault("threads", "5")));
        this.retries = Integer.max(1, Integer.parseInt(getOrDefault("retries", "5")));
        this.solrDocStoreUrl = URI.create(getOrFail("solr-doc-store-url"))
                .resolve("/api/holdings")
                .toString();
        this.jmxDomain = getOrDefault("jmx-domain", "metrics");
    }

    public String[] getQueues() {
        return queues;
    }

    public String getDatabaseThrottle() {
        return databaseThrottle;
    }

    public String getThrottle() {
        return throttle;
    }

    public long getEmptyQueueSleep() {
        return emptyQueueSleep;
    }

    public long getMaxQueryTime() {
        return maxQueryTime;
    }

    public int getIdleRescanEvery() {
        return idleRescanEvery;
    }

    public int getRescanEvery() {
        return rescanEvery;
    }

    public int getThreads() {
        return threads;
    }

    public int getRetries() {
        return retries;
    }

    public String getSolrDocStoreUrl() {
        return solrDocStoreUrl;
    }

    public String getJmxDomain() {
        return jmxDomain;
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

    @Override
    public String toString() {
        return "Config{" + "databaseThrottle=" + databaseThrottle + ", throttle=" + throttle + ", queues=" + Arrays.toString(queues) + ", emptyQueueSleep=" + emptyQueueSleep + ", maxQueryTime=" + maxQueryTime + ", idleRescanEvery=" + idleRescanEvery + ", rescanEvery=" + rescanEvery + ", threads=" + threads + ", retries=" + retries + ", solrDocStoreUrl=" + solrDocStoreUrl + '}';
    }

}
