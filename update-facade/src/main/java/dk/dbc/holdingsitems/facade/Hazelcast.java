package dk.dbc.holdingsitems.facade;

import com.hazelcast.collection.ISet;
import com.hazelcast.collection.ItemEvent;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hazelcast {

    private static final Logger log = LoggerFactory.getLogger(Hazelcast.class);

    private static final String AGENCYSET = "agencyset";

    private final HazelcastInstance instance;
    private boolean stopping = false;

    Hazelcast() {
        this(Map.of());
    }

    public Hazelcast(Map<String, String> extra) {
        HashMap<String, String> env = new HashMap<>(System.getenv());
        env.putAll(extra);
        try (InputStream is = substInFile("hz.xml", env)) {
            Config hzConfig = new XmlConfigBuilder(is).build();
            hzConfig.setInstanceName(getOrRaise(env, "HOSTNAME"));
            hzConfig.setClassLoader(Hazelcast.class.getClassLoader());
            this.instance = com.hazelcast.core.Hazelcast.newHazelcastInstance(hzConfig);
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownNode));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start hazelcast data instance", e);
        }
    }

    private static InputStream substInFile(String resourceName, Map<String, String> env) throws IOException {
        try (InputStream is = Hazelcast.class.getClassLoader().getResourceAsStream(resourceName)) {
            String input = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Matcher match = Pattern.compile("\\$\\{(\\w+)\\}", Pattern.DOTALL).matcher(input);
            StringBuilder content = new StringBuilder();
            int pos = 0;
            while (match.find()) {
                content.append(input.substring(pos, match.start()))
                        .append(getOrRaise(env, match.group(1)));
                pos = match.end();
            }
            content.append(input.substring(pos));
            return new ByteArrayInputStream(content.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String getOrRaise(Map<String, String> env, String match) {
        String value = env.get(match);
        if (value == null)
            throw new IllegalArgumentException("Required parameter: " + match + " is unset");
        return value;
    }

    public void shutdownNode() {
        log.warn("Hazelcast is shutting down");
        stopping = true;
        instance.shutdown();
    }

    public boolean isReady() {
        return !stopping && instance.getLifecycleService().isRunning() && instance.getPartitionService().isClusterSafe();
    }

    public ISet<Integer> getAgencySet() {
        return instance.getSet(AGENCYSET);
    }

    public <T> T withAgencyLock(int agencyId, Supplier<T> sup) {
        try (AgencyLock lock = registerAgency(agencyId)) {
            return sup.get();
        }
    }

    private AgencyLock registerAgency(int agencyId) {
        ISet<Integer> agencySet = getAgencySet();
        try (AgencyNotifierLock lock = new AgencyNotifierLock(agencySet, agencyId)) {
            synchronized (lock) {
                if (agencySet.add(agencyId)) {
                    return new AgencyLock(agencySet, agencyId);
                }
                for (int i = 0 ; i < 60 ; i++) {
                    lock.wait(1000);
                    if (agencySet.add(agencyId)) {
                        return new AgencyLock(agencySet, agencyId);
                    }
                }
            }
        } catch (InterruptedException ex) {
            throw new IllegalStateException("Could not get agency lock (interrupted)");
        }
        throw new IllegalStateException("Could not get agency lock");
    }

    private static final class AgencyLock implements AutoCloseable {

        private final ISet<Integer> agencySet;
        private final int agencyId;

        private AgencyLock(ISet<Integer> agencySet, int agencyId) {
            this.agencySet = agencySet;
            this.agencyId = agencyId;
        }

        @Override
        public void close() {
            agencySet.remove(agencyId);
        }
    }

    private static final class AgencyNotifierLock implements AutoCloseable, ItemListener<Integer> {

        private final ISet<Integer> agencySet;
        private final int agencyId;
        private final UUID uuid;

        private AgencyNotifierLock(ISet<Integer> agencySet, int agencyId) {
            this.agencySet = agencySet;
            this.agencyId = agencyId;
            this.uuid = agencySet.addItemListener(this, true);
        }

        @Override
        public void close() {
            agencySet.removeItemListener(uuid);
        }

        @Override
        public void itemAdded(ItemEvent<Integer> item) {
        }

        @Override
        public void itemRemoved(ItemEvent<Integer> item) {
            if (item.getItem() == agencyId) {
                synchronized (this) {
                    notify();
                }
            }
        }
    }
}
