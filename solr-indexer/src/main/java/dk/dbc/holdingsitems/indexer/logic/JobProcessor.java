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
package dk.dbc.holdingsitems.indexer.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.QueueJob;
import dk.dbc.holdingsitems.Record;
import dk.dbc.holdingsitems.RecordCollection;
import dk.dbc.holdingsitems.indexer.Config;
import dk.dbc.holdingsitems.indexer.monitor.Timed;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Stateless
public class JobProcessor {

    private static final Logger log = LoggerFactory.getLogger(JobProcessor.class);

    private static final ObjectMapper O = new ObjectMapper();
    private static final String VERSION = readVersion();

    private Client client;
    private UriBuilder uriBuilder;

    @Inject
    Config config;

    public JobProcessor() {
    }

    public JobProcessor(Config config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {

        client = ClientBuilder.newBuilder()
                .build();
        uriBuilder = UriBuilder.fromUri(config.getSolrDocStoreUrl());

    }

    @Timed
    public ObjectNode buildRequestJson(Connection connection, QueueJob job) throws Exception {
        String trackingId = job.getTrackingId();

        HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, trackingId, true);
        String bibliographicRecordId = job.getBibliographicRecordId();
        int agencyId = job.getAgencyId();
        Set<String> issueIds = dao.getIssueIds(bibliographicRecordId, agencyId);
        log.debug("issueIds = {}", issueIds);
        HashMap<UniqueFields, RepeatedFields> records = new HashMap<>();
        for (String issueId : issueIds) {
            RecordCollection collection = dao.getRecordCollection(bibliographicRecordId, agencyId, issueId);
            for (Iterator<Record> iterator = collection.iterator() ; iterator.hasNext() ;) {
                Record record = iterator.next();
                UniqueFields key = new UniqueFields(collection, record);
                RepeatedFields repeatedFields = records.computeIfAbsent(key, v -> new RepeatedFields(collection.getTrackingId(), job.getTrackingId()));
                repeatedFields.addItemId(record.getItemId());
                repeatedFields.addStatus(record.getStatus());
                repeatedFields.addTrackingId(record.getTrackingId());
            }
        }
        ObjectNode json = O.createObjectNode();
        addMetadata(json, agencyId, bibliographicRecordId, trackingId);
        ArrayNode jsonRecords = json.putArray("indexKeys");

        for (Map.Entry<UniqueFields, RepeatedFields> entry : records.entrySet()) {
            ObjectNode node = jsonRecords.addObject();
            entry.getKey().fillIn(node);
            entry.getValue().fillIn(node);
        }

        return json;
    }

    @Timed
    public JsonNode sendToSolrDocStore(JsonNode node) {
        ObjectNode res = O.createObjectNode();
        try {
            Response response = client.target(uriBuilder)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .buildPost(Entity.json(O.writeValueAsString(node)))
                    .invoke();
            res.put("status", response.getStatus());
            res.put("status-text", response.getStatusInfo().getReasonPhrase());
            res.put("content-type", response.getMediaType().toString());
            if (response.hasEntity()) {
                String body = response.readEntity(String.class);
                res.put("body", body);
                if (MediaType.APPLICATION_JSON_TYPE.equals(response.getMediaType())) {
                    res.set("json", O.readTree(body));
                }
            }
        } catch (IOException ex) {
            res.put("status", -1);
            res.put("status-text", ex.toString());
        }
        return res;
    }

    private void addMetadata(ObjectNode record, int agencyId, String bibliographicRecordId, String trackingId) {
        record.put("agencyId", agencyId);
        record.put("bibliographicRecordId", bibliographicRecordId);
        record.put("producerVersion", VERSION);
        record.put("trackingId", trackingId);
    }

    private static String readVersion() {
        String ret = "UNKNOWN";

        try (InputStream is = JobProcessor.class
                .getResourceAsStream("/version.txt")) {
            if (is != null) {
                byte[] buffer = new byte[256];
                int len = is.read(buffer); // Yes version is clipped at 256 bytes
                ret = new String(buffer, 0, len, StandardCharsets.UTF_8).trim();
            } else {
                throw new FileNotFoundException("version.txt");
            }
        } catch (IOException ex) {
            log.error("Error reading version text: {}", ex.getMessage());
            log.debug("Error reading version text:", ex);
        }
        return ret;
    }

}
