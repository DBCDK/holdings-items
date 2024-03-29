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
package dk.dbc.holdingsitems.kafkabridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.QueueJob;
import dk.dbc.holdingsitems.StateChangeMetadata;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.Status;
import dk.dbc.kafka.testutil.KafkaContainer;
import dk.dbc.kafka.testutil.KafkaContainerTools;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class WorkerIT extends JpaBase {

    private static final Logger log = LoggerFactory.getLogger(WorkerIT.class);
    private static final ObjectMapper O = new ObjectMapper();

    private static final String TOPIC = "my-topic";

    @ClassRule
    public static final KafkaContainer kafkaContainer = new KafkaContainer();
    public static final KafkaContainerTools kafka = kafkaContainer.tools();

    @Before
    public void setUp() throws Exception {

        kafka.recreateTopic(TOPIC, "-p 1 -r 1");
    }

    @Test(timeout = 30_000L)
    public void test() throws Exception {
        System.out.println("TESTING");

        jpa(em -> {

            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, "track1");
            BibliographicItemEntity b = dao.getRecordCollection("12345678", 654321, Instant.now());
            IssueEntity issue1 = fill(b.issue("Issue#1", Instant.now()));
            issue1.setIssueText("#1");
            issue1.setReadyForLoan(1);
            fill(issue1.item("1234", Instant.MIN))
                    .setStatus(Status.ON_SHELF);
            fill(issue1.item("2345", Instant.MIN))
                    .setStatus(Status.ON_LOAN);
            issue1.save();
            IssueEntity issue2 = fill(b.issue("Issue#2", Instant.now()));
            issue2.setIssueText("#2");
            issue2.setReadyForLoan(1);
            fill(issue2.item("5432", Instant.MIN))
                    .setStatus(Status.ON_SHELF);
            fill(issue2.item("4321", Instant.MIN))
                    .setStatus(Status.ON_ORDER);
            issue2.save();
            b.save();
        });

        jpa(em -> {
            JobProcessor kafkaWorker = new JobProcessor();
            Config config = new Config() {
                @Override
                public String getKafkaServers() {
                    return kafka.bootstrapServers();
                }

                @Override
                public String getKafkaTopic() {
                    return TOPIC;
                }
            };
            KafkaSender sender = new KafkaSender();
            sender.config = config;
            sender.init();

            kafkaWorker.config = config;
            kafkaWorker.em = em;
            kafkaWorker.sender = sender;

            HashMap<String, StateChangeMetadata> stateChange = new HashMap<>();
            stateChange.put("1234", new StateChangeMetadata(Status.ON_LOAN, Status.ON_SHELF, Instant.parse("2018-01-01T12:34:56Z")));

            kafkaWorker.transferJob(new QueueJob(654321, "12345678", "", "t1"));
            kafkaWorker.transferJob(new QueueJob(123456, "87654321", O.writeValueAsString(stateChange), "t1"));
        });

        Map<String, List<String>> records = kafka.consume(TOPIC);

        System.out.println("records = " + records);
        JsonNode tree;

        assertTrue("has 12345678", records.containsKey("12345678"));
        tree = O.readTree(records.get("12345678").get(0));
        System.out.println("tree = " + tree);
        assertEquals(true, tree.at("/complete").asBoolean(false));
        assertEquals("OnShelf", tree.at("/items/1234/oldStatus").asText());
        assertEquals("OnShelf", tree.at("/items/1234/newStatus").asText());
        assertEquals("OnLoan", tree.at("/items/2345/oldStatus").asText());
        assertEquals("OnLoan", tree.at("/items/2345/newStatus").asText());
        assertEquals("OnShelf", tree.at("/items/5432/oldStatus").asText());
        assertEquals("OnShelf", tree.at("/items/5432/newStatus").asText());
        assertEquals("OnOrder", tree.at("/items/4321/oldStatus").asText());
        assertEquals("OnOrder", tree.at("/items/4321/newStatus").asText());

        assertTrue("has 87654321", records.containsKey("87654321"));
        tree = O.readTree(records.get("87654321").get(0));
        System.out.println("tree = " + tree);
        assertEquals(false, tree.at("/complete").asBoolean(true));
        assertEquals("OnShelf", tree.at("/items/1234/oldStatus").asText());
        assertEquals("OnLoan", tree.at("/items/1234/newStatus").asText());

        assertEquals(2, records.size());
    }

}
