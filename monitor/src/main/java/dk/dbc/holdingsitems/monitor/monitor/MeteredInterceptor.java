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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Metered
@Interceptor
public class MeteredInterceptor {

    private static final Logger log = LoggerFactory.getLogger(MeteredInterceptor.class);

    private static final ConcurrentHashMap<Method, Meter> METERS = new ConcurrentHashMap<>();

    @Inject
    JmxMetrics metrics;

    @AroundInvoke
    public Object timer(InvocationContext ic) throws Exception {
        Method method = ic.getMethod();
        Meter meter = METERS.computeIfAbsent(method, this::makeMeter);
        meter.mark();
        return ic.proceed();
    }

    private Meter makeMeter(Method method) {
        log.debug("method = {}", method);
        String methodName = method.getName();
        Metered metered = method.getAnnotation(Metered.class);
        if (metered != null && !metered.value().isEmpty()) {
            methodName = metered.value();
        }
        String name = MetricRegistry.name(method.getDeclaringClass(), methodName);
        return metrics.getRegistry().meter(name);
    }

}
