/*
 * Copyright (C) 2017-2018 DBC A/S (http://dbc.dk/)
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
package dk.dbc.holdingsitems.update;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.Record;
import dk.dbc.holdingsitems.RecordCollection;
import dk.dbc.log.LogWith;
import dk.dbc.oss.ns.holdingsitemsupdate.Authentication;
import dk.dbc.oss.ns.holdingsitemsupdate.BibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.Holding;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItem;
import dk.dbc.oss.ns.holdingsitemsupdate.ModificationTimeStamp;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineBibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.StatusType;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.xml.datatype.XMLGregorianCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public abstract class UpdateRequest {

    private static final Logger log = LoggerFactory.getLogger(UpdateRequest.class);

    private static final ObjectMapper O = new ObjectMapper();
    private static final ObjectNode EMPTY_OBJECT_NODE = O.createObjectNode();
//    private HashMap<String, ObjectNode> biblStateChange;

    /**
     * Get an authentication soap class from a request
     *
     * @return authentication class
     */
    public abstract Authentication getAuthentication();

    /**
     * extract tracking id from incoming request
     *
     * @return tracking id
     */
    public abstract String getTrakingId();

    /**
     * List queues a request should enqueue onto
     *
     * @return comma separated list of queues for this endpoint
     */
    public abstract String getQueueListOld();

    /**
     * List queues a request should enqueue onto
     *
     * @return comma separated list of queues for this endpoint
     */
    public abstract String getQueueList();

    /**
     * Actually process the request content
     */
    public abstract void processBibliograhicItems();

    private final UpdateWebservice updateWebService;
    private final HashSet<QueueEntry> queueEntries;
    private final HashMap<String, HashMap<String, UpdateMetadata>> oldItemStatus; // Bibl -> item -> status
    protected HoldingsItemsDAO dao;

    /**
     * Update request super class
     *
     * @param updateWebservice used for accessing timers
     */
    public UpdateRequest(UpdateWebservice updateWebservice) {
        this.updateWebService = updateWebservice;
        this.queueEntries = new HashSet<>();
        this.oldItemStatus = new HashMap<>();
//        this.biblStateChange = new HashMap<>();
    }

    /**
     * Set a HoldingsItems DAO
     *
     * @param dao newly created dao
     */
    public void setDao(HoldingsItemsDAO dao) {
        this.dao = dao;
    }

    /**
     * Collect queue jobs
     *
     * @param bibliographicRecordId jobId
     * @param agencyId              jobId
     */
    public void addQueueJob(String bibliographicRecordId, int agencyId) {
        queueEntries.add(new QueueEntry(agencyId, bibliographicRecordId));
    }

    /**
     * Send all cached queue jobs to the queue
     */
    protected void queue() {
        String[] queues = getQueueList().split("[^-0-9a-zA-Z_]+");
        for (QueueEntry queueEntry : queueEntries) {
            for (String queue : queues) {
                if (queue.isEmpty()) {
                    continue;
                }
                log.info("QUEUE: " +
                         queueEntry.getAgencyId() + "|" +
                         queueEntry.getBibliographicRecordId() + "|" +
                         queue);
                try {
                    String saveStateChangeText = "{}";
                    try {
                        HashMap<String, UpdateMetadata> saveStateChange = oldItemStatus.computeIfAbsent(queueEntry.getBibliographicRecordId(), f -> new HashMap<>());
                        saveStateChangeText = O.writeValueAsString(saveStateChange);
                    } catch (JsonProcessingException ex) {
                        log.error("Cannot make json to string: {}", ex.getMessage());
                        log.debug("Cannot make json to string: ", ex);
                    }
                    dao.enqueue(queueEntry.getBibliographicRecordId(), queueEntry.getAgencyId(), saveStateChangeText, queue);
                } catch (HoldingsItemsException ex) {
                    throw new WrapperException(ex);
                }
            }
        }
        queues = getQueueListOld().split("[^-0-9a-zA-Z_]+");
        for (QueueEntry queueEntry : queueEntries) {
            for (String queue : queues) {
                if (queue.isEmpty()) {
                    continue;
                }
                log.info("QUEUE: " +
                         queueEntry.getAgencyId() + "|" +
                         queueEntry.getBibliographicRecordId() + "|" +
                         queue);
                try {
                    dao.enqueueOld(queueEntry.getBibliographicRecordId(), queueEntry.getAgencyId(), queue);
                } catch (HoldingsItemsException ex) {
                    throw new WrapperException(ex);
                }
            }
        }
    }

    /**
     * Fetch a collection and add a holding to it
     *
     * @param modified              whence the request says the collection is
     *                              modified
     * @param agencyId              id
     * @param bibliographicRecordId id
     * @param note                  note about content
     * @param holding               holding describing an issue
     * @param complete              is this a complete og an update request
     * @throws WrapperException RuntimeException for wrapping Exceptions in
     *                          (useful for using .stream())
     */
    protected void processHolding(Timestamp modified, int agencyId, String bibliographicRecordId, String note, boolean complete, Holding holding) throws WrapperException {
        try (LogWith logWith = new LogWith()) {
            String issueId = holding.getIssueId();
            logWith.with("issueId", issueId);
            log.info("agencyId = " + agencyId + "; bibliographicRecordId = " + bibliographicRecordId + "; issueId = " + issueId + "; trackingId = " + getTrakingId());
            RecordCollection collection = getRecordCollection(bibliographicRecordId, agencyId, issueId, modified);
            collection.setComplete(complete);
            if (complete) {
                HashMap<String, UpdateMetadata> statuses = oldItemStatus.computeIfAbsent(bibliographicRecordId, f -> new HashMap<>());
                for (UpdateMetadata metadata : statuses.values()) {
                    System.out.println("StatusType.DECOMMISSIONED.value() = " + StatusType.DECOMMISSIONED.value());
                    metadata.update(StatusType.DECOMMISSIONED, modified);
                }
                for (Record record : collection) {
                    record.setStatus(StatusType.DECOMMISSIONED.value());
                }
            }
            collection.setNote(note);
            copyValue(holding::getIssueText, collection::setIssueText);
            XMLGregorianCalendar expectedDeliveryDate = holding.getExpectedDeliveryDate();
            if (expectedDeliveryDate == null) {
                collection.setExpectedDelivery(null);
            } else {
                collection.setExpectedDelivery(toDate(expectedDeliveryDate, true));
            }
            collection.setReadyForLoan(holding.getReadyForLoan().intValueExact());
            holding.getHoldingsItems().forEach(item -> addItemToCollection(collection, item, modified));
            log.debug("saving");
            saveCollection(collection, modified);
        } catch (HoldingsItemsException ex) {
            throw new WrapperException(ex);
        }
    }

    /**
     * Fetch a collection and record old item states
     *
     * @param bibliographicRecordId
     * @param agencyId
     * @param issueId
     * @param modified
     * @return
     * @throws HoldingsItemsException
     */
    protected RecordCollection getRecordCollection(String bibliographicRecordId, int agencyId, String issueId, Timestamp modified) throws HoldingsItemsException {
        try (Timer.Context time = updateWebService.loadCollectionTimer.time()) {
            RecordCollection collection = dao.getRecordCollectionForUpdate(
                    bibliographicRecordId,
                    agencyId,
                    issueId,
                    modified);
            HashMap<String, UpdateMetadata> biblItemStatus = oldItemStatus.computeIfAbsent(bibliographicRecordId, b -> new HashMap<>());
            for (Record record : collection) {
                biblItemStatus.putIfAbsent(record.getItemId(), new UpdateMetadata(StatusType.fromValue(record.getStatus()), modified));
            }
            log.debug("oldItemStatus = {}", oldItemStatus);
            return collection;
        }
    }

    /**
     * call collection.save(modified), and compute state changes pr. itemid
     *
     * @param collection collection to save
     * @param modified   timestamp it has been modified
     * @throws HoldingsItemsException save (database) error
     */
    protected void saveCollection(RecordCollection collection, Timestamp modified) throws HoldingsItemsException {
        log.debug("saveCollection {}", collection);
        try (Timer.Context time = updateWebService.saveCollectionTimer.time()) {
            collection.save(modified);
        }
        String bibliographicRecordId = collection.getBibliographicRecordId();
//        HashMap<String, UpdateMetadata> biblItemStatus = oldItemStatus.computeIfAbsent(bibliographicRecordId, b -> new HashMap<>());
//        ObjectNode base = biblStateChange.computeIfAbsent(bibliographicRecordId, b -> O.createObjectNode());
        HashMap<String, UpdateMetadata> metas = oldItemStatus.computeIfAbsent(bibliographicRecordId, f -> new HashMap<>());
        log.debug("metas = {}", metas);
        for (Record record : collection) {
            log.debug("record = {}", record);
//            String itemId = record.getItemId();
//            String status = record.getStatus();

//            String oldStatus = biblItemStatus.getOrDefault(itemId, "UNKNOWN");
//            if (status.equals(oldStatus)) {
//                if (base.has(itemId)) {
//                    base.remove(itemId);
//                }
//            } else {
//                if (base.has(itemId)) {
//                    ObjectNode itemReport = (ObjectNode) base.get(itemId);
//                    itemReport.put("is", status);
//                } else {
//                    ObjectNode itemReport = base.putObject(itemId);
//                    itemReport.put("was", oldStatus);
//                    itemReport.put("is", status);
//                }
//            }
        }
//        log.debug("this.biblStateChange = {}", this.biblStateChange);
    }

    /**
     * Add a HoldingsItem to a collection
     *
     * @param collection target collection
     * @param item       item to add
     */
    protected void addItemToCollection(RecordCollection collection, HoldingsItem item, Timestamp modified) {
        try (LogWith logWith = new LogWith()) {
            String itemId = item.getItemId();
            logWith.with("itemId", itemId);
            log.info("Adding item: {}", itemId);
            Record rec = collection.findRecord(itemId);
            XMLGregorianCalendar accessionDate = item.getAccessionDate();
            if (accessionDate != null) {
                rec.setAccessionDate(toDate(accessionDate, false));
            }
            StatusType status = item.getStatus();
            if (status != null) {
                if (status == StatusType.ONLINE) {
                    throw new FailedUpdateInternalException("Use endpoint onlineHoldingsItemsUpdate got status ONLINE");
                }
                rec.setStatus(status.value());
                HashMap<String, UpdateMetadata> metas = oldItemStatus.computeIfAbsent(collection.getBibliographicRecordId(), f -> new HashMap<>());
                UpdateMetadata meta = metas.computeIfAbsent(itemId, f -> new UpdateMetadata(modified));
                meta.update(status, modified);
            }
            copyValue(item::getBranch, rec::setBranch);
            copyValue(item::getCirculationRule, rec::setCirculationRule);
            copyValue(item::getDepartment, rec::setDepartment);
            copyValue(item::getLocation, rec::setLocation);
            copyValue(item::getSubLocation, rec::setSubLocation);
        }
    }

    /**
     * Copy a value if it isn't null
     *
     * @param <T>  type of the value
     * @param from where to take the value
     * @param to   where to put it
     */
    private <T> void copyValue(Supplier<T> from, Consumer<T> to) {
        T value = from.get();
        if (value != null) {
            to.accept(value);
        }
    }

    /**
     * Construct a Timestamp that is usable with time zone UTC, from a
     * webservice element
     *
     * @param timestamp request element
     * @return sql type timestamp in UTC
     */
    protected Timestamp parseTimestamp(ModificationTimeStamp timestamp) {
        return Timestamp.valueOf(timestamp
                .getModificationDateTime()
                .toGregorianCalendar()
                .toInstant()
                .plus(timestamp.getModificationMilliSeconds(),
                      ChronoUnit.MILLIS)
                .atOffset(ZoneOffset.UTC)
                .toLocalDateTime());
    }

    /**
     * Construct a date form a XML element
     *
     * @param date            XML element date
     * @param failIfInThePast raise InvalidDeliveryDateException if date is in
     *                        the past
     * @return Java date object
     */
    protected Date toDate(XMLGregorianCalendar date, boolean failIfInThePast) {
        GregorianCalendar gregorianCalendar = date.toGregorianCalendar();
        Instant instant = gregorianCalendar.toInstant().plusMillis(gregorianCalendar.getTimeZone().getRawOffset()).truncatedTo(ChronoUnit.DAYS);
        if (failIfInThePast) {
            Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);
            if (instant.isBefore(today)) {
                throw new InvalidDeliveryDateException("expected delivery date in the past");
            }
        }
        return Date.from(instant);
    }

    /**
     * Used for sorting bibliographic items/holdings to avoid deadlocks
     */
    protected static final Comparator<BibliographicItem> BIBLIOGRAPHICITEM_SORT_COMPARE = new Comparator<BibliographicItem>() {
        @Override
        public int compare(BibliographicItem o1, BibliographicItem o2) {
            return o1.getBibliographicRecordId().compareTo(o2.getBibliographicRecordId());
        }
    };
    protected static final Comparator<Holding> HOLDINGS_SORT_COMPARE = new Comparator<Holding>() {
        @Override
        public int compare(Holding o1, Holding o2) {
            return o1.getIssueId().compareTo(o2.getIssueId());
        }
    };
    protected static final Comparator<OnlineBibliographicItem> ONLINE_BIBLIOGRAPHICITEM_SORT_COMPARE = new Comparator<OnlineBibliographicItem>() {
        @Override
        public int compare(OnlineBibliographicItem l, OnlineBibliographicItem r) {
            return l.getBibliographicRecordId().compareTo(r.getBibliographicRecordId());
        }
    };

    /**
     * Simple queue entry wrapper, only data structure, no logic
     */
    private static class QueueEntry {

        private final int agencyId;
        private final String bibliographicRecordId;

        public QueueEntry(int agencyId, String bibliographicRecordId) {
            this.agencyId = agencyId;
            this.bibliographicRecordId = bibliographicRecordId;
        }

        public int getAgencyId() {
            return agencyId;
        }

        public String getBibliographicRecordId() {
            return bibliographicRecordId;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 37 * hash + this.agencyId;
            hash = 37 * hash + Objects.hashCode(this.bibliographicRecordId);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final QueueEntry other = (QueueEntry) obj;
            if (this.agencyId != other.agencyId) {
                return false;
            }
            if (!Objects.equals(this.bibliographicRecordId, other.bibliographicRecordId)) {
                return false;
            }
            return true;
        }

    }

    /**
     * Simple queue entry wrapper, only data structure, no logic
     */
    private static class QueueEntryIssue {

        private final int agencyId;
        private final String bibliographicRecordId;
        private final String issueId;

        public QueueEntryIssue(int agencyId, String bibliographicRecordId, String issueId) {
            this.agencyId = agencyId;
            this.bibliographicRecordId = bibliographicRecordId;
            this.issueId = issueId;
        }

        public int getAgencyId() {
            return agencyId;
        }

        public String getBibliographicRecordId() {
            return bibliographicRecordId;
        }

        public String getIssueId() {
            return issueId;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 37 * hash + this.agencyId;
            hash = 37 * hash + Objects.hashCode(this.bibliographicRecordId);
            hash = 37 * hash + Objects.hashCode(this.issueId);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final QueueEntryIssue other = (QueueEntryIssue) obj;
            if (this.agencyId != other.agencyId) {
                return false;
            }
            if (!Objects.equals(this.bibliographicRecordId, other.bibliographicRecordId)) {
                return false;
            }
            if (!Objects.equals(this.issueId, other.issueId)) {
                return false;
            }
            return true;
        }

    }

}
