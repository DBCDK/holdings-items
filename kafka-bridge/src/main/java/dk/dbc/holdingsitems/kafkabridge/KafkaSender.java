/*
 * Copyright (C) 2021 DBC A/S (http://dbc.dk/)
 *
 * This is part of pgqueue-to-kafka-service
 *
 * pgqueue-to-kafka-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * pgqueue-to-kafka-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.kafkabridge;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
public class KafkaSender {

    private static final Logger log = LoggerFactory.getLogger(KafkaSender.class);

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Inject
    Config config;

    private KafkaProducer<String, String> producer;

    @PostConstruct
    public void init() {
        int instanceNo = COUNTER.incrementAndGet();
        Properties producerProps = new Properties();
        String clientId = UUID.randomUUID().toString();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getKafkaServers());
        producerProps.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        producerProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1024*1024);
        producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, clientId + "-" + instanceNo);
        producerProps.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, 120_000);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        this.producer = new KafkaProducer<>(producerProps);
        this.producer.initTransactions();
    }

    @Timed
    public void send(String workId, String content) {
        producer.beginTransaction();
        String topic = config.getKafkaTopic();
        ArrayList<Exception> result = new ArrayList<>(1);
        Callback cb = (metadata, exception) -> {
            if (exception != null) {
                log.error("Kafka exception: {}", exception.getMessage());
            }
            synchronized (result) {
                result.add(exception);
            }
        };
        producer.send(new ProducerRecord<>(topic, workId, content), cb);
        producer.commitTransaction();
        Optional<Exception> exception = result.stream().filter(e -> e != null)
                .findFirst();
        if (exception.isPresent()) {
            throw new EJBException("Kafka transaction aborted: " + exception.get().getMessage());
        }
    }
}
