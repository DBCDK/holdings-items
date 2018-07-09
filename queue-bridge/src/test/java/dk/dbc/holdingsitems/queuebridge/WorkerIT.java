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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.holdingsitems.QueueJob;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Session;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.server.config.ConnectionFactoryConfiguration;
import org.hornetq.jms.server.config.JMSConfiguration;
import org.hornetq.jms.server.config.JMSQueueConfiguration;
import org.hornetq.jms.server.config.impl.ConnectionFactoryConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSQueueConfigurationImpl;
import org.hornetq.jms.server.embedded.EmbeddedJMS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class WorkerIT {

    private static final Logger log = LoggerFactory.getLogger(WorkerIT.class);

    private static final ObjectMapper O = new ObjectMapper();

    private static final String JMS_QUEUE_NAME = "myQueue";

    private JMSContext context;
    private String port;
    private JMSConsumer consumer;
    private EmbeddedJMS broker;
    private ConnectionFactory connectionFactory;

    public WorkerIT() {
    }

    @Before
    public void setUp() throws Exception {
        this.port = System.getProperty("hornetq.port", "17777");
        startBroker();
        this.context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE);
        this.consumer = context.createConsumer(context.createQueue(JMS_QUEUE_NAME));
        this.context.start();
    }

    @After
    public void tearDown() throws Exception {
        this.consumer.close();
        this.context.stop();
        this.context.close();
    }

    @Test(timeout = 2000L)
    public void testInit() throws Exception {
        System.out.println("init");

        JobProcessor jobProcessor = new JobProcessor();
        jobProcessor.context = context;
        jobProcessor.mapping = new HashMap<>();
        jobProcessor.mapping.put("my-queue", "myQueue");
        jobProcessor.producer = context.createProducer();

        QueueJob queueJob = new QueueJob(700000, "87654321", "{}", "T#1");
        jobProcessor.transferJob(queueJob, "my-queue");

        Message msg = consumer.receive(1000L);
        String body = msg.getBody(String.class);
        System.out.println("body = " + body);

        JsonNode tree = O.readTree(body);
        JsonNode agencyId = tree.get("agencyId");
        assertNotNull(agencyId);
        assertEquals(700000, agencyId.asInt());
        JsonNode bibliographicRecordId = tree.get("bibliographicRecordId");
        assertNotNull(bibliographicRecordId);
        assertEquals("87654321", bibliographicRecordId.asText());
    }

    private void startBroker() throws Exception {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(TransportConstants.PORT_PROP_NAME, Integer.parseInt(port));
        parameters.put(TransportConstants.HOST_PROP_NAME, "127.0.0.1");

        Configuration configuration = new ConfigurationImpl();
        configuration.setPersistenceEnabled(false);
        configuration.setSecurityEnabled(false);
        configuration.getAcceptorConfigurations()
                .add(new TransportConfiguration(NettyAcceptorFactory.class.getName(),
                                                parameters));
        configuration.setPersistenceEnabled(false);

        TransportConfiguration connectorConfig = new TransportConfiguration(
                NettyConnectorFactory.class.getName(),
                parameters);
        configuration.getConnectorConfigurations()
                .put("connector", connectorConfig);

        JMSConfiguration jmsConfig = new JMSConfigurationImpl();

        //Configure connection factory
        List<String> connectorNames = new ArrayList<>();
        connectorNames.add("connector");
        ConnectionFactoryConfiguration cfConfig = new ConnectionFactoryConfigurationImpl(
                "cf", false, connectorNames,
                "/cf");
        jmsConfig.getConnectionFactoryConfigurations().add(cfConfig);

        //Configure queues
        JMSQueueConfiguration queueConfig = new JMSQueueConfigurationImpl(JMS_QUEUE_NAME, null, false, JMS_QUEUE_NAME);
        jmsConfig.getQueueConfigurations().add(queueConfig);

        //Start broker
        this.broker = new EmbeddedJMS();
        broker.setConfiguration(configuration);
        broker.setJmsConfiguration(jmsConfig);
        System.out.println("starting");
        broker.start();
        System.out.println("started");
        this.connectionFactory = (ConnectionFactory) broker.lookup("/cf");
    }

}
