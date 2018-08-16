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
package dk.dbc.holdingsitems.queuebridge;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
    public static final String PROPERTIES = "holdings-items-queue-bridge";

    private Properties props;
    private String[] queues;
    private int threads;
    private String mqServer;
    private String jmxDomain;
    private Map<String, String> mapping;

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

    private static final Pattern RULES_PATTERN = Pattern.compile("[-\\w]+=[-\\w]+(\\s*,\\s*[-\\w]+=[-\\w]+)*");

    @PostConstruct
    public void init() {
        log.info("Setting up config");
        if (props == null) {
            props = findProperties(PROPERTIES);
        }

        String rules = getOrFail("rules");
        if (!RULES_PATTERN.matcher(rules).matches()) {
            throw new EJBException("Invalid rule set: " + rules);
        }
        this.queues = Arrays.stream(rules.split("\\s*,\\s*"))
                .map(s -> s.split("=", 2)[1])
                .toArray(String[]::new);

        this.mapping = Arrays.stream(rules.split("\\s*,\\s*"))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(a -> a[1], a -> a[0]));
        this.threads = Integer.max(1, Integer.parseInt(getOrDefault("threads", "1")));
        this.mqServer = getOrFail("queue-server");
        this.jmxDomain = getOrDefault("jmx-domain", "metrics");
    }

    public String[] getQueues() {
        return queues;
    }


    public int getThreads() {
        return threads;
    }

    public String getMqServer() {
        return mqServer;
    }

    public Map<String, String> getMapping() {
        return Collections.unmodifiableMap(mapping);
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
        return "Config{" + "queues=" + Arrays.toString(queues) + ", threads=" + threads + ", mqServer=" + mqServer + ", jmxDomain=" + jmxDomain + ", mapping=" + mapping + '}';
    }



}
