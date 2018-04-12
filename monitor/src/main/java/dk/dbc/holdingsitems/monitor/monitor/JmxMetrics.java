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
package dk.dbc.holdingsitems.monitor.monitor;

import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import dk.dbc.holdingsitems.monitor.Config;
import java.lang.reflect.Member;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Singleton
@Startup
public class JmxMetrics {

    private static final Logger log = LoggerFactory.getLogger(JmxMetrics.class);

    @Inject
    Config config;

    private final MetricRegistry registry;
    private JmxReporter reporter;

    public JmxMetrics() {
        registry = new MetricRegistry();
    }

    @PostConstruct
    public void init() {
        log.info("Start JMX Reporter");
        reporter = JmxReporter.forRegistry(registry)
                .inDomain(config.getJmxDomain())
                .build();
        reporter.start();
    }

    @PreDestroy
    public void destroy() {
        reporter.stop();
    }

    public MetricRegistry getRegistry() {
        return registry;
    }

    @Produces
    public Timer makeTimer(InjectionPoint ip) {
        Member member = ip.getMember();
        String variableName = member.getName();
        if (variableName.endsWith("Timer")) {
            int length = variableName.length() - "Timer".length();
            variableName = variableName.substring(0, length);
        }
        String name = MetricRegistry.name(member.getDeclaringClass(), variableName);
        return registry.timer(name);
    }

    @Produces
    public Meter makeMeter(InjectionPoint ip) {
        Member member = ip.getMember();
        String variableName = member.getName();
        if (variableName.endsWith("Meter")) {
            int length = variableName.length() - "Meter".length();
            variableName = variableName.substring(0, length);
        }
        String name = MetricRegistry.name(member.getDeclaringClass(), variableName);
        return registry.meter(name);
    }

    @Produces
    public Counter makeCounter(InjectionPoint ip) {
        Member member = ip.getMember();
        String variableName = member.getName();
        if (variableName.endsWith("Counter")) {
            int length = variableName.length() - "Counter".length();
            variableName = variableName.substring(0, length);
        }
        String name = MetricRegistry.name(member.getDeclaringClass(), variableName);
        return registry.counter(name);
    }

}
