/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of dbc-monitor [unloadable]
 *
 * dbc-monitor [unloadable] is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dbc-monitor [unloadable] is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.monitor;

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
    public static final String PROPERTIES = "holdings-items-monitor";

    private Properties props;
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

    public String getJmxDomain() {
        return jmxDomain;
    }

    @PostConstruct
    public void init() {
        log.info("Setting up config");
        if (props == null) {
            props = findProperties(PROPERTIES);
        }
        this.jmxDomain = getOrDefault("jmx-domain", "metrics");
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

}
