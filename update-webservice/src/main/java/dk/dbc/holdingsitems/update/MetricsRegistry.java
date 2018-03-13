/*
 * Copyright (C) 2015-2018 DBC A/S (http://dbc.dk/)
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
package dk.dbc.holdingsitems.update;

import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Statistics logger
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Singleton
public class MetricsRegistry {

    private static final Logger log = LoggerFactory.getLogger(MetricsRegistry.class);

    private final MetricRegistry metrics;

    private JmxReporter reporter;

    /**
     * Create a codahale metrics registry
     */
    public MetricsRegistry() {
        this.metrics = new MetricRegistry();
    }

    @PostConstruct
    public void create() {
        log.debug("PostConstruct");
        reporter = JmxReporter.forRegistry(metrics).build();
        reporter.start();
    }

    @PreDestroy
    public void destroy() {
        if (reporter != null) {
            reporter.stop();
        }
    }

    /**
     * Injectable counter
     *
     * @param ip injection point, tells about variable name aso.
     * @return newly created counter
     */
    @Produces
    public Counter getCounter(InjectionPoint ip) {
        String name = ip.getMember().getName();
        if (name.endsWith("Counter")) {
            name = name.substring(0, name.length() - "Counter".length());
        }
        return metrics.counter(MetricRegistry.name(ip.getMember().getDeclaringClass(), name));
    }


    /**
     * Injectable timer
     *
     * @param ip injection point, tells about variable name aso.
     * @return newly created timer
     */
    @Produces
    public Timer getTimer(InjectionPoint ip) {
        String name = ip.getMember().getName();
        if (name.endsWith("Timer")) {
            name = name.substring(0, name.length() - "Timer".length());
        }
        return metrics.timer(MetricRegistry.name(ip.getMember().getDeclaringClass(), name));
    }
}
