package dk.dbc.holdingsitems.update;

import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
// @Singleton @Startup to ensure the counters are ready as soon as the war is deployed
// Remember to register all counters in primeMetricCounters()
@Singleton
@Startup
@Lock(LockType.READ)
public class StatusCounterBean {

    @Inject
    MetricRegistry mr;

    private final Tag service = new Tag("service", "holdings-items-update");

    @PostConstruct
    public void primeMetricCounters() {
        List.of("update", "online", "complete")
                .forEach(n ->
                        List.of("success", "failure")
                                .forEach(s -> counter(s, n)));
    }

    /**
     * Create an auto-closable context that will register this request as failed unless told otherwise
     *
     * @param name name of the request
     * @return context
     */
    public Context count(String name) {
        Counter successCounter = counter("success", name);
        Counter failureCounter = counter("failure", name);
        return new Context(successCounter, failureCounter);
    }

    private Counter counter(String status, String name) {
        return mr.counter("request-" + status, service, new Tag("request", name));
    }

    public static class Context implements AutoCloseable {

        private final Counter successCounter;
        private final Counter failureCounter;
        private boolean failure;

        private Context(Counter successCounter, Counter failureCounter) {
            this.successCounter = successCounter;
            this.failureCounter = failureCounter;
            this.failure = true;
        }

        /**
         * Register the request as a success instead of a failure
         */
        public void success() {
            failure = false;
        }

        @Override
        public void close() {
            if (failure) {
                failureCounter.inc();
            } else {
                successCounter.inc();
            }
        }
    }
}
