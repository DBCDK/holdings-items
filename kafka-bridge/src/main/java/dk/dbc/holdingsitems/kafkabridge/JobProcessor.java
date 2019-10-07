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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dbc.ee.stats.Timed;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.QueueJob;
import dk.dbc.holdingsitems.StateChangeMetadata;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.kafka.producer.Producer;
import dk.dbc.log.LogWith;
import java.util.HashMap;
import java.util.Set;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
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

    @Inject
    EntityManager em;

    @Timed
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void transferJob(QueueJob job) throws Exception {
        try (Producer messageTarget = makeKafkaTarget() ;
             LogWith logWith = new LogWith(job.getTrackingId())) {
            String stateChange = job.getStateChange();
            ObjectNode message = O.createObjectNode();
            message.put("agencyId", job.getAgencyId());
            message.put("bibliographicRecordId", job.getBibliographicRecordId());
            message.put("trackingId", job.getTrackingId());
            log.info("processing: {}:{}", job.getAgencyId(), job.getBibliographicRecordId());
            if (stateChange.isEmpty() ||
                stateChange.charAt(0) != '{' ||
                stateChange.startsWith("{}")) {
                message.put("complete", true);
                ObjectNode obj = buildFullStatus(job);
                message.set("items", obj);
            } else {
                message.put("complete", false);
                ObjectNode obj = (ObjectNode) O.readTree(stateChange);
                message.set("items", obj);
            }
            String json = O.writeValueAsString(message);
            log.debug("posting: {}", json);
            messageTarget.send(job.getBibliographicRecordId(), json);
        }
    }

    Producer makeKafkaTarget() {
        return Producer.builder()
                .withServers(config.getKafkaServers())
                .withTopic(config.getKafkaTopic())
                .build();
    }

    private ObjectNode buildFullStatus(QueueJob job) throws HoldingsItemsException {
        HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, job.getTrackingId());
        String bibliographicRecordId = job.getBibliographicRecordId();
        int agencyId = job.getAgencyId();
        Set<String> issueIds = dao.getIssueIds(bibliographicRecordId, agencyId);
        log.debug("issueIds = {}", issueIds);
        HashMap<String, StateChangeMetadata> stateChange = new HashMap<>();

        BibliographicItemEntity b = dao.getRecordCollectionUnLocked(bibliographicRecordId, agencyId, null);
        if (b.isNew())
            throw new IllegalStateException("Nothing found in database related to: " + agencyId + ":" + bibliographicRecordId);

        b.stream().forEach(issue -> {
            issue.stream().forEach(item -> {
                stateChange.put(item.getItemId(), new StateChangeMetadata(item.getStatus(), item.getModified()));
            });
        });

        return O.valueToTree(stateChange);
    }
}
