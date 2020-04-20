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
package dk.dbc.holdingsitems.indexer;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dbc.commons.testutils.postgres.connection.PostgresITDataSource;
import dk.dbc.holdingsitems.QueueJob;
import dk.dbc.holdingsitems.indexer.logic.JobProcessor;
import dk.dbc.pgqueue.PreparedQueueSupplier;
import dk.dbc.pgqueue.QueueSupplier;
import dk.dbc.pgqueue.consumer.JobMetaData;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;
import static java.sql.Types.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class WorkerIT extends JpaBase {

    private static final ObjectMapper O = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(WorkerIT.class);

    private PostgresITDataSource pgds;
    private Config config;
    private static int solrDocStorePort;
    private static Server jettyServer;
    private static Consumer consumer;

    @BeforeClass
    public static void setUpClass() throws Exception {
        solrDocStorePort = Integer.parseInt(System.getProperty("solr-doc-store.port", "8877"));
        jettyServer = new Server(solrDocStorePort);
        consumer = new Consumer();
        jettyServer.setHandler(consumer);
        jettyServer.start();
    }

    @Before
    public void setUp() throws Exception {
        config = new Config("queues=q1,q2",
                            "solr-doc-store-url=http://localhost:" + solrDocStorePort + "/");
        config.init();
        log.debug("config = {}", config);

        consumer.requests.clear();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        jettyServer.stop();
        jettyServer.join();
    }

    @Test
    public void testJetty() throws Exception {
        log.info("testJetty");
        Client c = ClientBuilder.newClient();
        Invocation invocation = c.target("http://localhost:" + solrDocStorePort + "/foo/bar")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .buildPost(Entity.json("{\"foo\":\"bar\"}"));
        ObjectNode responseJson = process(invocation);
        assertJson(200, responseJson, "/status");
        assertJson(true, responseJson, "/body/ok");
        ObjectNode request = consumer.requests.poll();
        assertNotNull(request);
        assertJson("bar", request, "/body/foo");
    }

    @Test
    public void testResponse() throws Exception {
        log.info("testResponse");
        JobProcessor jobProcessor = new JobProcessor(config, null);
        jobProcessor.init();
        JsonNode node = O.readTree("{\"hello\":\"there\"}");
        JsonNode response = jobProcessor.sendToSolrDocStore(node);
        log.debug("response = {}", response);
        assertNotNull(response);
        assertJson(200, response, "/status");
        ObjectNode request = consumer.requests.poll();
        log.debug("request = {}", request);
        assertNotNull(request);
        assertJson("there", request, "/body/hello");
    }

    @Test
    public void testBuildRequest() throws Exception {
        System.out.println("testBuildRequest");
        try (Connection connection = dataSource.getConnection()) {
            insert(connection, "inserts/insert1.json");
        }

        jpa(em -> {
            JobProcessor jobProcessor = new JobProcessor(config, em);
            jobProcessor.init();
            try (Connection connection = dataSource.getConnection()) {
                ObjectNode json = jobProcessor.buildRequestJson(new QueueJob(700000, "87654321", "{}", "T#1"));
                System.out.println("json = " + json);
                assertJson(700000, json, "/agencyId");
                assertJson("87654321", json, "/bibliographicRecordId");
                assertTrue(json.get("indexKeys").isArray());
                ArrayNode subDocs = (ArrayNode) json.get("indexKeys");
                assertEquals(3, subDocs.size());
                ObjectNode dp = findSubDocContaining(subDocs, "/holdingsitem.itemId", "dp1");
                log.debug("dp = {}", dp);
                assertEquals("Two entries collapsed", 2, dp.get("holdingsitem.itemId").size());
                assertEquals("Two entries Different status", 2, dp.get("holdingsitem.status").size());
                ObjectNode rpo1 = findSubDocContaining(subDocs, "/holdingsitem.itemId", "rpo1");
                log.debug("rpo1 = {}", rpo1);
                assertEquals(O.readTree("{\"holdingsitem.note\":[\"any-text\"]," +
                                        "\"holdingsitem.firstAccessionDate\":[\"2019-03-04T00:00:00Z\"]," +
                                        "\"holdingsitem.agencyId\":[\"700000\"]," +
                                        "\"holdingsitem.bibliographicRecordId\":[\"87654321\"]," +
                                        "\"rec.bibliographicRecordId\":[\"87654321\"]," +
                                        "\"holdingsitem.issueId\":[\"rpo\"]," +
                                        "\"holdingsitem.issueText\":[\"ready player one\"]," +
                                        "\"holdingsitem.readyForLoan\":[\"-1\"]," +
                                        "\"holdingsitem.branch\":[\"Here\"]," +
                                        "\"holdingsitem.branchId\":[\"345678\"]," +
                                        "\"holdingsitem.department\":[\"\"]," +
                                        "\"holdingsitem.location\":[\"\"]," +
                                        "\"holdingsitem.subLocation\":[\"\"]," +
                                        "\"holdingsitem.circulationRule\":[\"\"]," +
                                        "\"holdingsitem.accessionDate\":[\"2016-02-03T00:00:00Z\"]," +
                                        "\"holdingsitem.collectionId\":[\"700000-87654321\"]," +
                                        "\"holdingsitem.itemId\":[\"rpo1\"]," +
                                        "\"holdingsitem.status\":[\"OnLoan\"]," +
                                        "\"rec.trackingId\":[\"\",\"T#1\",\"xxx\"]}"),
                             rpo1);
                ObjectNode rpo2 = findSubDocContaining(subDocs, "/holdingsitem.itemId", "rpo2");
                log.debug("rpo2 = {}", rpo2);
                assertEquals(O.readTree("{\"holdingsitem.note\":[\"any-text\"]," +
                                        "\"holdingsitem.firstAccessionDate\":[\"2019-03-04T00:00:00Z\"]," +
                                        "\"holdingsitem.agencyId\":[\"700000\"]," +
                                        "\"holdingsitem.bibliographicRecordId\":[\"87654321\"]," +
                                        "\"rec.bibliographicRecordId\":[\"87654321\"]," +
                                        "\"holdingsitem.issueId\":[\"rpo\"]," +
                                        "\"holdingsitem.issueText\":[\"ready player one\"]," +
                                        "\"holdingsitem.readyForLoan\":[\"-1\"]," +
                                        "\"holdingsitem.branch\":[\"There\"]," +
                                        "\"holdingsitem.branchId\":[\"456789\"]," +
                                        "\"holdingsitem.department\":[\"\"]," +
                                        "\"holdingsitem.location\":[\"\"]," +
                                        "\"holdingsitem.subLocation\":[\"\"]," +
                                        "\"holdingsitem.circulationRule\":[\"\"]," +
                                        "\"holdingsitem.accessionDate\":[\"2015-01-02T00:00:00Z\"]," +
                                        "\"holdingsitem.loanRestriction\":[\"e\"]," +
                                        "\"holdingsitem.collectionId\":[\"700000-87654321\"]," +
                                        "\"holdingsitem.itemId\":[\"rpo2\"]," +
                                        "\"holdingsitem.status\":[\"OnShelf\"]," +
                                        "\"rec.trackingId\":[\"\",\"T#1\",\"xxx\"]}"),
                             rpo2);
            }
        });
    }

    @Test
    public void testConsumerDequeues() throws Exception {
        log.info("testConsumerDequeues");

        try (Connection connection = dataSource.getConnection()) {
            PreparedQueueSupplier<QueueJob> supplier = new QueueSupplier<>(QueueJob.STORAGE_ABSTRACTION)
                    .preparedSupplier(connection);
            supplier.enqueue("q1", new QueueJob(700000, "87654321", "{}", "foo1"));
        }
        BlockingQueue<QueueJob> jobs = new LinkedBlockingQueue<>();

        Worker worker = new Worker() {
            @Override
            public void work(Connection connection, QueueJob job, JobMetaData metaData) {
                jobs.offer(job);
            }
        };
        worker.config = config;
        worker.dataSource = dataSource;
        worker.metrics = new MetricRegistry();
        worker.init();
        QueueJob job = jobs.poll(10, TimeUnit.SECONDS);
        worker.destroy();
        assertNotNull(job);
    }

    @Test
    public void testConsumer() throws Exception {
        log.info("testConsumer");
        try (Connection connection = dataSource.getConnection()) {
            insert(connection, "inserts/insert1.json");
        }

        try (Connection connection = dataSource.getConnection()) {
            PreparedQueueSupplier<QueueJob> supplier = new QueueSupplier<>(QueueJob.STORAGE_ABSTRACTION)
                    .preparedSupplier(connection);
            supplier.enqueue("q1", new QueueJob(700000, "87654321", "{}", "foo1"));
        }

        jpa(em -> {
            Worker worker = new Worker();
            worker.config = config;
            worker.dataSource = dataSource;
            worker.jobProcessor = new JobProcessor(config, em);
            worker.metrics = new MetricRegistry();
            worker.jobProcessor.init();
            worker.init();
            ObjectNode job = consumer.requests.poll(10, TimeUnit.SECONDS);
            log.debug("job = {}", job);
            worker.destroy();
            assertNotNull(job);

            JsonNode json = job.get("body");
            assertTrue(json.get("indexKeys").isArray());
            ArrayNode subDocs = (ArrayNode) json.get("indexKeys");
            assertEquals(3, subDocs.size());
            // Content tested in: testBuildRequest
        });
    }

    private ObjectNode findSubDocContaining(ArrayNode nodes, String path, String value) {
        for (JsonNode node : nodes) {
            if (node.isObject()) {
                JsonNode targetNode = node.at(path);
                if (targetNode.isArray()) {
                    for (JsonNode target : targetNode) {
                        log.debug("target.asText() = {}", target.asText());
                        if (value.equals(target.asText())) {
                            return (ObjectNode) node;
                        }
                    }
                }
                if (value.equals(targetNode.asText())) {
                    return (ObjectNode) node;
                }
            }
        }
        fail("could not find node containing: " + value + " at " + path);
        return null;
    }

    private JsonNode jsonResource(String resource) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
        if (is != null) {
            return O.readTree(is);
        } else {
            return O.createArrayNode();
        }
    }

    private void insert(Connection connection, String resource) throws SQLException, IOException {
        JsonNode json = jsonResource(resource);
        if (json.isArray()) {
            for (JsonNode node : json) {
                if (node.isObject()) {
                    insert(connection, (ObjectNode) node);
                } else {
                    fail("cannot insert: " + node);
                }
            }
        } else if (json.isObject()) {
            insert(connection, (ObjectNode) json);
        } else {
            fail("cannot insert: " + json);
        }
    }

    private void insert(Connection connection, ObjectNode content) throws SQLException {
        ArrayList<String> fields = new ArrayList<>();
        for (Iterator<String> fieldNames = content.fieldNames() ; fieldNames.hasNext() ;) {
            String fieldName = fieldNames.next();
            if (!fieldName.isEmpty()) {
                fields.add(fieldName);
            }
        }
        String sql = new StringBuilder()
                .append("INSERT INTO ")
                .append(content.get("").asText())
                .append("(")
                .append(fields.stream()
                        .collect(Collectors.joining(", ")))
                .append(") VALUES(")
                .append(fields.stream()
                        .map(s -> "?")
                        .collect(Collectors.joining(", ")))
                .append(")")
                .toString();
        log.trace("sql = {}", sql);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int col = 0;
            for (String field : fields) {
                int parameterType = stmt.getParameterMetaData().getParameterType(++col);
                JsonNode value = content.get(field);
                log.trace("field = {}; value = {}", field, value);
                if (value.isNull()) {
                    stmt.setNull(col, parameterType);
                } else {
                    switch (parameterType) {
                        case BIGINT:
                        case INTEGER:
                        case NUMERIC:
                        case SMALLINT:
                            stmt.setInt(col, value.asInt());
                            break;
                        case DATE:
                            if (value.asText().isEmpty()) {
                                stmt.setDate(col, Date.valueOf(LocalDate.now()));

                            } else {
                                stmt.setDate(col, Date.valueOf(value.asText("")));
                            }
                            break;
                        case TIME:
                        case TIME_WITH_TIMEZONE:
                            if (value.asText().isEmpty()) {
                                stmt.setTime(col, Time.valueOf(LocalTime.now()));
                            } else {
                                stmt.setTime(col, Time.valueOf(value.asText("")));
                            }
                            break;
                        case TIMESTAMP:
                        case TIMESTAMP_WITH_TIMEZONE:
                            if (value.asText().isEmpty()) {
                                stmt.setTimestamp(col, Timestamp.valueOf(LocalDateTime.now()));
                            } else {
                                stmt.setTimestamp(col, Timestamp.valueOf(value.asText("")));
                            }
                            break;
                        default:
                            stmt.setString(col, value.asText());
                            break;
                    }
                }
            }
            stmt.executeUpdate();
        }
    }

    private ObjectNode process(Invocation request) throws IOException {
        Response response = request.invoke();
        ObjectNode responseJson = O.createObjectNode();
        responseJson.put("status", response.getStatus());
        if (response.hasEntity()) {
            String entity = response.readEntity(String.class);
            if (response.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
                responseJson.set("body", O.readTree(entity));
            } else {
                responseJson.put("", entity);
            }
        }
        log.debug("responseJson = {}", responseJson);
        return responseJson;
    }

    private static void assertJson(boolean expected, JsonNode actual, String path) {
        actual = actual.at(path);
        assertTrue(actual.isBoolean());
        assertEquals(expected, actual.asBoolean());
    }

    private static void assertJson(String expected, JsonNode actual, String path) {
        actual = actual.at(path);
        assertTrue(actual.isTextual());
        assertEquals(expected, actual.asText());
    }

    private static void assertJson(int expected, JsonNode actual, String path) {
        actual = actual.at(path);
        assertTrue(actual.isIntegralNumber());
        assertEquals(expected, actual.asInt());
    }

    private static class Consumer extends AbstractHandler {

        private static final ObjectMapper O = new ObjectMapper();

        private final BlockingQueue<ObjectNode> requests = new LinkedBlockingQueue<>();

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

            ObjectNode data = O.createObjectNode();
            data.put("uri", target);
            if (request.getMethod().equals("POST")) {
                try (ServletInputStream is = request.getInputStream()) {
                    JsonNode tree = O.readTree(is);
                    data.set("body", tree);
                }
            }
            log.debug("data = {}", data);
            requests.add(data);

            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            response.getWriter()
                    .print("{\"ok\":true}");
        }

    }

}
