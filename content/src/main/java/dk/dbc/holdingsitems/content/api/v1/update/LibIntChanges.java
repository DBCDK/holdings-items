package dk.dbc.holdingsitems.content.api.v1.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.Status;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibIntChanges {

    private static final Logger log = LoggerFactory.getLogger(LibIntChanges.class);
    private static final ObjectMapper O = JsonMapper.builder().build();

    private final BibliographicItemEntity entity;
    private final Map<String, Status> oldStatus;

    public LibIntChanges(BibliographicItemEntity entity) {
        this.entity = entity;
        this.oldStatus = entity.stream()
                .flatMap(IssueEntity::stream)
                .collect(Collectors.toMap(ItemEntity::getItemId, ItemEntity::getStatus));
    }

    public String report(boolean complete) {
        Map<String, Status> newStatus = entity.stream()
                .flatMap(IssueEntity::stream)
                .collect(Collectors.toMap(ItemEntity::getItemId, ItemEntity::getStatus));
        Map<String, Instant> modified = entity.stream()
                .flatMap(IssueEntity::stream)
                .collect(Collectors.toMap(ItemEntity::getItemId, ItemEntity::getModified));

        ObjectNode json = O.createObjectNode()
                .put("agencyId", entity.getAgencyId())
                .put("bibliographicRecordId", entity.getBibliographicRecordId())
                .put("trackingId", entity.getTrackingId())
                .put("complete", complete);
        ObjectNode items = json.putObject("items");

        newStatus.forEach((itemId, status) -> {
            Status was = oldStatus.get(itemId);
            if (was == null) {
                items.putObject(itemId)
                        .put("newStatus", status.toString())
                        .put("when", modified.get(itemId).toString());
            } else if (was != status) {
                items.putObject(itemId)
                        .put("newStatus", status.toString())
                        .put("oldStatus", was.toString())
                        .put("when", modified.get(itemId).toString());
            }
        });
        oldStatus.forEach((itemId, status) -> {
            if (!newStatus.containsKey(itemId)) {
                items.putObject(itemId)
                        .put("newStatus", Status.DECOMMISSIONED.toString())
                        .put("oldStatus", status.toString())
                        .put("when", entity.getModified().toString());
            }
        });
        try {
            String value = O.writeValueAsString(json);
            log.debug("json report: {}", value);
            return value;
        } catch (JsonProcessingException ex) {
            log.error("This is really bad - cannot write JSON: {}", ex.getMessage());
            log.debug("This is really bad - cannot write JSON: ", ex);
            return "{}";
        }
    }
}
