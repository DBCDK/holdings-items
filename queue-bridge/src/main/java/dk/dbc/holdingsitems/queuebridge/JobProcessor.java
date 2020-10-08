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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.messaging.ConnectionConfiguration;
import com.sun.messaging.ConnectionFactory;
import dk.dbc.holdingsitems.QueueJob;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class JobProcessor {

    private static final Logger log = LoggerFactory.getLogger(JobProcessor.class);

    private static final ObjectMapper O = new ObjectMapper();

    @Inject
    Config config;

    Map<String, String> mapping;
    JMSContext context;
    JMSProducer producer;

    @PostConstruct
    public void init() {

        log.info("Create JMS Context");
        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setProperty(ConnectionConfiguration.imqAddressList, config.getMqServer());
            this.context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE);
            this.producer = context.createProducer();
        } catch (JMSException ex) {
            log.error("Error createing JMS Context: {}", ex.getMessage());
            log.debug("Error createing JMS Context: ", ex);
            throw new EJBException("Error creating JMX Context", ex);
        }

        this.mapping = config.getMapping();
    }

    @Timed(reusable = true)
    public void transferJob(QueueJob job, String consumer) throws Exception {
        ObjectNode node = O.createObjectNode();
        String bibliographicRecordId = job.getBibliographicRecordId();
        int agencyId = job.getAgencyId();
        node.put("bibliographicRecordId", bibliographicRecordId);
        node.put("agencyId", agencyId);
        String json = O.writeValueAsString(node);
        TextMessage message = context.createTextMessage(json);
        message.setStringProperty("bibliographicRecordId", bibliographicRecordId);
        message.setStringProperty("agencyId", String.valueOf(agencyId));

        String target = mapping.get(consumer);
        log.debug("sending {} to {}", json, target);
        Queue queue = context.createQueue(target);
        producer.send(queue, message);
    }

}
