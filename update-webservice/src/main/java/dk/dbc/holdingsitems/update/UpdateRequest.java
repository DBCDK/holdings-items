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

import dk.dbc.holdingsitems.StateChangeMetadata;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.Status;
import dk.dbc.log.LogWith;
import dk.dbc.oss.ns.holdingsitemsupdate.Authentication;
import dk.dbc.oss.ns.holdingsitemsupdate.BibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.Holding;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItem;
import dk.dbc.oss.ns.holdingsitemsupdate.ModificationTimeStamp;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineBibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.StatusType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.xml.datatype.XMLGregorianCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.time.ZoneOffset.UTC;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public abstract class UpdateRequest {

    private static final Logger log = LoggerFactory.getLogger(UpdateRequest.class);

    private static final ObjectMapper O = new ObjectMapper();

    /**
     * Get an authentication soap class from a request
     *
     * @return authentication class
     */
    public abstract Authentication getAuthentication();

    /**
     * Retrieve agencyId from request as string
     *
     * @return agency id
     */
    public abstract int getAgencyId();

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
    protected final HashMap<String, HashMap<String, StateChangeMetadata>> oldItemStatus; // Bibl -> item -> status
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
    }

    /**
     * Set a HoldingsItems DAO
     *
     * @param dao newly created dao
     */
    public void setDao(HoldingsItemsDAO dao) {
        this.dao = dao;
    }

    public void updateNote(BibliographicItemEntity item, String note, Instant modified) {
        if (item.isNew() ||
            !item.getModified().isAfter(modified))
            item.setNote(note);
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
                        HashMap<String, StateChangeMetadata> saveStateChange = oldItemStatus.computeIfAbsent(queueEntry.getBibliographicRecordId(), f -> new HashMap<>());
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
     * @param modified whence the request says the collection is
     *                 modified
     * @param bibItem  database representation
     * @param holding  holding describing an issue
     * @param complete is this a complete or an update request
     * @throws WrapperException RuntimeException for wrapping Exceptions in
     *                          (useful for using .stream())
     */
    protected void processHolding(Instant modified, BibliographicItemEntity bibItem, boolean complete, Holding holding) throws WrapperException {
        String issueId = holding.getIssueId();
        try (LogWith logWith = new LogWith()) {
            logWith.with("issueId", issueId);
            log.info("agencyId = {}; bibliographicRecordId = {}; issueId = {}; trackingId = {}",
                     bibItem.getAgencyId(),
                     bibItem.getBibliographicRecordId(),
                     issueId,
                     getTrakingId());
            IssueEntity issue = bibItem.issue(issueId, modified);
            if (issue.isNew())
                issue.setCreated(Instant.now());
            if (!issue.isNew() && issue.getComplete().isAfter(modified))
                return;

            if (issue.isNew() || !issue.getModified().isAfter(modified)) {
                copyValue(holding::getIssueText, issue::setIssueText);

                XMLGregorianCalendar expectedDeliveryDate = holding.getExpectedDeliveryDate();
                if (expectedDeliveryDate == null) {
                    issue.setExpectedDelivery(null);
                } else {
                    issue.setExpectedDelivery(toDate(expectedDeliveryDate, true));
                }
                issue.setReadyForLoan(holding.getReadyForLoan().intValueExact())
                        .setModified(modified)
                        .setUpdated(Instant.now())
                        .setTrackingId(getTrakingId());
            }

            HashSet<String> handledItems = new HashSet<>();
            holding.getHoldingsItems().forEach(holdingsItem -> {
                handledItems.add(holdingsItem.getItemId());
                addItemToCollection(issue, holdingsItem, modified);
            });
            HashMap<String, StateChangeMetadata> statuses = oldItemStatus.computeIfAbsent(bibItem.getBibliographicRecordId(), f -> new HashMap<>());
            if (complete) {
                issue.setComplete(modified);
                issue.stream()
                        .filter(item -> !handledItems.contains(item.getItemId())) // Skip all that are in the request
                        .filter(item -> !item.getModified().isAfter(modified)) // Skip all that are newer in tha database
                        .filter(item -> item.getStatus() != Status.ONLINE) // Online are special - should not be deleted
                        .forEach(item -> {
                            statuses.computeIfAbsent(item.getItemId(), i -> new StateChangeMetadata(item.getStatus(), item.getModified()))
                                    .update(Status.DECOMMISSIONED, modified);
                            item.setStatus(Status.DECOMMISSIONED)
                                    .setModified(modified)
                                    .setTrackingId(getTrakingId());
                        });
            }
        }
    }

    /**
     * Set all values to Decommissioned for a collection, and set complete
     * timestamp
     *
     * @param collection the collection (bibid/agency/issue)
     * @param modified   then data has been changed
     */
    public void decommissionEntireHolding(IssueEntity collection, Instant modified) {
        collection.setComplete(modified);
        String bibliographicRecordId = collection.getBibliographicRecordId();
        HashMap<String, StateChangeMetadata> statuses = oldItemStatus.computeIfAbsent(bibliographicRecordId, f -> new HashMap<>());
        collection.stream()
                .forEach(record -> {
                    String itemId = record.getItemId();
                    Status oldStatus = record.getStatus();
                    if (oldStatus == Status.ONLINE)
                        return;
                    if (record.getModified().isAfter(modified)) // This is modified after complete - keep it
                        return;

                    StateChangeMetadata metadata = statuses.computeIfAbsent(itemId, i -> new StateChangeMetadata(oldStatus, record.getModified()));
                    metadata.update(Status.DECOMMISSIONED, modified);
                    record.setStatus(Status.DECOMMISSIONED);
                    record.setModified(modified);
                    record.setTrackingId(getTrakingId());

                    log.debug("record = {}", record);
                    log.debug("metadata = {}", metadata);
                });
    }

    /**
     * Fetch a collection and record old item states
     *
     * @param bibliographicRecordId part of the primary key
     * @param agencyId              part of the primary key
     * @param issueId               part of the primary key
     * @param modified              set modification time on the collection
     * @return Collection fetched from database or created
     * @throws HoldingsItemsException if database communication fails
     */
    protected IssueEntity getRecordCollection(String bibliographicRecordId, int agencyId, String issueId, Instant modified) throws HoldingsItemsException {
        try (Timer.Context time = updateWebService.loadCollectionTimer.time()) {
            BibliographicItemEntity bibItem = dao.getRecordCollection(bibliographicRecordId,
                                                                      agencyId,
                                                                      modified);
            bibItem.setTrackingId(getTrakingId());
            IssueEntity collection = bibItem.issue(issueId, modified);
            HashMap<String, StateChangeMetadata> biblItemStatus = oldItemStatus.computeIfAbsent(bibliographicRecordId, b -> new HashMap<>());
            collection.stream()
                    .forEach(record -> biblItemStatus.putIfAbsent(record.getItemId(), new StateChangeMetadata(record.getStatus(), modified)));
            log.debug("oldItemStatus = {}", oldItemStatus);
            collection.setTrackingId(getTrakingId());
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
    protected void saveCollection(IssueEntity collection, Instant modified) throws HoldingsItemsException {
        log.debug("saveCollection {}", collection);
        try (Timer.Context time = updateWebService.saveCollectionTimer.time()) {
            collection.setModified(modified);
            collection.setUpdated(Instant.now());
            collection.save();
            System.out.println("collection = " + collection + " <--------------------------------------------");
        }
    }

    /**
     * Add a HoldingsItem to a collection
     *
     * @param issue        target collection
     * @param holdingsItem item to add
     * @param modified     timestamp it has been modified
     */
    protected void addItemToCollection(IssueEntity issue, HoldingsItem holdingsItem, Instant modified) {
        try (LogWith logWith = new LogWith()) {
            String itemId = holdingsItem.getItemId();
            logWith.with("itemId", itemId);
            log.info("Adding item: {}", itemId);
            ItemEntity item = issue.item(itemId, modified);
            item.setTrackingId(getTrakingId());
            XMLGregorianCalendar accessionDate = holdingsItem.getAccessionDate();
            if (accessionDate != null) {
                item.setAccessionDate(toDate(accessionDate, false));
            }
            StatusType status = holdingsItem.getStatus();
            if (status != null) {
                if (status == StatusType.ONLINE) {
                    throw new FailedUpdateInternalException("Use endpoint onlineHoldingsItemsUpdate got status ONLINE");
                }
                oldItemStatus.computeIfAbsent(issue.getBibliographicRecordId(),
                                              f -> new HashMap<>())
                        .computeIfAbsent(itemId, f -> new StateChangeMetadata(
                                         item.getStatus(), item.getModified()))
                        .update(Status.parse(status.value()), modified);
                item.setStatus(Status.parse(status.value()));
            }
            item.setModified(modified);
            copyValue(holdingsItem::getBranch, item::setBranch);
            copyValue(holdingsItem::getBranchId, item::setBranchId);
            copyValue(holdingsItem::getCirculationRule, item::setCirculationRule);
            copyValue(holdingsItem::getLoanRestriction, item::setLoanRestriction);
            copyValue(holdingsItem::getDepartment, item::setDepartment);
            copyValue(holdingsItem::getLocation, item::setLocation);
            copyValue(holdingsItem::getSubLocation, item::setSubLocation);
        }
    }

    /**
     * Copy a value if it isn't null
     *
     * @param <T>  type of the value
     * @param from where to take the value
     * @param to   where to put it
     */
    protected <T> void copyValue(Supplier<T> from, Consumer<T> to) {
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
    protected Instant parseTimestamp(ModificationTimeStamp timestamp) {
        return timestamp
                .getModificationDateTime()
                .toGregorianCalendar()
                .toInstant()
                .plusMillis(timestamp.getModificationMilliSeconds());
    }

    /**
     * Construct a date form a XML element
     *
     * @param date            XML element date
     * @param failIfInThePast raise InvalidDeliveryDateException if date is in
     *                        the past
     * @return Java date object
     */
    protected LocalDate toDate(XMLGregorianCalendar date, boolean failIfInThePast) {
        GregorianCalendar gregorianCalendar = date.toGregorianCalendar();
        Instant instant = gregorianCalendar.toInstant().plusMillis(gregorianCalendar.getTimeZone().getRawOffset()).truncatedTo(ChronoUnit.DAYS);
        if (failIfInThePast) {
            Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);
            if (instant.isBefore(today)) {
                throw new InvalidDeliveryDateException("expected delivery date in the past");
            }
        }
        return LocalDateTime.ofInstant(instant, UTC).toLocalDate();
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
