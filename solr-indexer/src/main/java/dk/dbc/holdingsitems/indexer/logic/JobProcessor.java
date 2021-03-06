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
import dk.dbc.holdingsitems.indexer.Config;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import java.io.IOException;
import java.util.HashMap;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.eclipse.microprofile.metrics.annotation.Timed;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Stateless
public class JobProcessor {

    private static final ObjectMapper O = new ObjectMapper();

    private Client client;
    private UriBuilder uriBuilder;

    @Inject
    Config config;

    @Inject
    EntityManager em;

    public JobProcessor() {
    }

    public JobProcessor(Config config, EntityManager em) {
        this.config = config;
        this.em = em;
    }

    @PostConstruct
    public void init() {
        client = ClientBuilder.newBuilder()
                .build();
        uriBuilder = UriBuilder.fromUri(config.getSolrDocStoreUrl());
    }

    @Timed
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ObjectNode buildRequestJson(QueueJob job) throws Exception {
        String trackingId = job.getTrackingId();

        HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, trackingId);
        String bibliographicRecordId = job.getBibliographicRecordId();
        int agencyId = job.getAgencyId();
        BibliographicItemEntity b = dao.getRecordCollectionUnLocked(bibliographicRecordId, agencyId);

        ObjectNode json = O.createObjectNode();
        addMetadata(json, agencyId, bibliographicRecordId, trackingId);
        ArrayNode jsonRecords = json.putArray("indexKeys");

        if (b != null) {
            HashMap<UniqueFields, RepeatedFields> records = new HashMap<>();
            b.stream().forEach(issue -> {
                issue.stream().forEach(item -> {
                    UniqueFields key = new UniqueFields(issue, item);
                    RepeatedFields repeatedFields = records.computeIfAbsent(key, v -> new RepeatedFields(b.getTrackingId(), issue.getTrackingId(), job.getTrackingId()));
                    repeatedFields.addRepeatedFieldsFrom(item);
                });
            });

            records.forEach((unique, repeated) -> {
                ObjectNode node = jsonRecords.addObject();
                node.putArray(SolrFields.NOTE.getFieldName()).add(b.getNote());
                node.putArray(SolrFields.FIRST_ACCESSION_DATE.getFieldName()).add(UniqueFields.isoDate(b.getFirstAccessionDate()));
                unique.fillIn(node);
                repeated.fillIn(node);
            });
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
        record.put("trackingId", trackingId);
    }
}
