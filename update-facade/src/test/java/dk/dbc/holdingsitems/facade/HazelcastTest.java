package dk.dbc.holdingsitems.facade;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HazelcastTest {

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    public void testAgencyLock() throws Exception {
        System.out.println("testAgencyLock");

        Hazelcast hazelcast = new Hazelcast(Map.of("HAZELCAST_CLUSTER", "localhost",
                                                   "HOSTNAME", "localhost"));
        for (int i = 0 ; i < 600 ; i++) {
            Thread.sleep(100);
            if (hazelcast.isReady())
                break;
        }
        assertThat(hazelcast.isReady(), is(true));

        AtomicBoolean requiredAgain = new AtomicBoolean(false);

        Thread thread = new Thread(() -> {
            System.out.println("Trying for agency");
            hazelcast.withAgencyLock(123456, () -> {
                                 System.out.println("Got lock #2");
                                 synchronized (requiredAgain) {
                                     requiredAgain.set(true);
                                     requiredAgain.notify();
                                 }
                                 System.out.println("Releasing lock #2");
                                 return null;
                             });
        });
        hazelcast.withAgencyLock(123456, () -> {
                             System.out.println("Got lock #1");
                             thread.start(); // Try to aquire lock simultaneously
                             try {
                                 Thread.sleep(1000);
                             } catch (InterruptedException ex) {
                                 System.err.println(ex);
                             }
                             System.out.println("Releasing lock #1");
                             return null;
                         });
        synchronized (requiredAgain) {
            for (int j = 0 ; j < 10 && !requiredAgain.get() ; j++) {
                requiredAgain.wait();
            }
        }
        thread.join();
        assertThat(requiredAgain.get(), is(true));
        hazelcast.shutdownNode();
    }
}
