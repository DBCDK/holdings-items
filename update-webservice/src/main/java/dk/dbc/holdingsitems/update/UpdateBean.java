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

import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.StateChangeMetadata;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.Status;
import dk.dbc.log.LogWith;
import dk.dbc.oss.ns.holdingsitemsupdate.Authentication;
import dk.dbc.oss.ns.holdingsitemsupdate.BibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteBibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteHoldingsItemsUpdate;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteHoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.Holding;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItem;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdate;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateResponse;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateResult;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateStatusEnum;
import dk.dbc.oss.ns.holdingsitemsupdate.ModificationTimeStamp;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineBibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineHoldingsItemsUpdate;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineHoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.StatusType;
import java.io.StringWriter;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

import static java.util.Collections.EMPTY_SET;
import static java.util.stream.Collectors.toList;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Stateless
public class UpdateBean {

    private static final Logger log = LoggerFactory.getLogger(UpdateBean.class);

    @Inject
    Config config;

    @Inject
    AccessValidator validator;

    @PersistenceContext(unitName = "holdingsItems_PU")
    EntityManager em;

    private WebServiceContext wsc;

    @Inject
    MetricRegistry metric;

    Counter requestCounter;
    Counter requestUpdateCounter;
    Counter requestCompleteCounter;
    Counter requestOnlineCounter;
    Counter requestInvalidCounter;
    Counter requestSystemErrorCounter;
    Counter requestAuthenticationErrorCounter;
    Timer loadCollectionTimer;
    Timer saveCollectionTimer;

    private Function<Object, String> mapToXml;

    @PostConstruct
    public void init() {
        requestCounter = metric.counter(getClass().getCanonicalName() + "request");
        requestUpdateCounter = metric.counter(getClass().getCanonicalName() + "requestUpdate");
        requestCompleteCounter = metric.counter(getClass().getCanonicalName() + "requestComplete");
        requestOnlineCounter = metric.counter(getClass().getCanonicalName() + "requestOnline");
        requestInvalidCounter = metric.counter(getClass().getCanonicalName() + "requestInvalid");
        requestSystemErrorCounter = metric.counter(getClass().getCanonicalName() + "requestSystemError");
        requestAuthenticationErrorCounter = metric.counter(getClass().getCanonicalName() + "requestAuthenticationError");
        loadCollectionTimer = metric.timer(getClass().getCanonicalName() + "loadCollection");
        saveCollectionTimer = metric.timer(getClass().getCanonicalName() + "saveCollection");

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(
                    Authentication.class,
                    BibliographicItem.class,
                    CompleteBibliographicItem.class,
                    CompleteHoldingsItemsUpdate.class,
                    CompleteHoldingsItemsUpdateRequest.class,
                    Holding.class,
                    HoldingsItem.class,
                    HoldingsItemsUpdate.class,
                    HoldingsItemsUpdateRequest.class,
                    HoldingsItemsUpdateResponse.class,
                    HoldingsItemsUpdateResult.class,
                    HoldingsItemsUpdateStatusEnum.class,
                    ModificationTimeStamp.class,
                    OnlineBibliographicItem.class,
                    OnlineHoldingsItemsUpdate.class,
                    OnlineHoldingsItemsUpdateRequest.class,
                    StatusType.class);
            mapToXml = (o) -> {
                try {
                    StringWriter sw = new StringWriter();
                    jaxbContext.createMarshaller()
                            .marshal(o, sw);
                    return sw.toString();
                } catch (JAXBException ex) {
                    return "[NO XML GENERATED " + ex.getMessage() + "]";
                }
            };
        } catch (JAXBException ex) {
            log.error("Error creating JAXBContext: {}", ex.getMessage());
            log.debug("Error creating JAXBContext: ", ex);
            mapToXml = (o) -> "[NO XML GENERATED " + ex.getMessage() + "]";
        }
    }

    public void setWebServiceContext(WebServiceContext wsc) {
        this.wsc = wsc;
    }

    // CPD-OFF
    /**
     * Accept request for multiple bibliographic record ids
     * <p>
     * This is for small updates
     *
     * @param req soap request
     * @return soap response
     */
    @Timed
    public HoldingsItemsUpdateResult holdingsItemsUpdate(final HoldingsItemsUpdateRequest req) {
        requestUpdateCounter.inc();
        if (req.getTrackingId() == null || req.getTrackingId().isEmpty()) {
            String trackingId = UUID.randomUUID().toString();
            log.info("Setting tracking id to: {}", trackingId);
            req.setTrackingId(trackingId);
        }
        try (LogWith logWith = new LogWith(req.getTrackingId())) {
            logWith.agencyId(req.getAgencyId());

            logXml(req.getAgencyId(), req, req.getAuthentication());
            return handleRequest(new UpdateRequest(this) {

                @Override
                public Authentication getAuthentication() {
                    return req.getAuthentication();
                }

                @Override
                public int getAgencyId() {
                    return req.getAgencyId();
                }

                @Override
                public String getTrakingId() {
                    return req.getTrackingId();
                }

                @Override
                public Optional<String> getQueueSupplierName() {
                    return config.getUpdateSupplier();
                }

                @Override
                public Optional<String> getOriginalQueueSupplierName() {
                    return config.getUpdateOriginalSupplier();
                }

                /**
                 * iterate over bibliographic record items, and call
                 * processJHolding
                 * upon them
                 */
                @Override
                public void processBibliograhicItems() {
                    log.debug("update");
                    req.getBibliographicItems().stream()
                            .sorted(BIBLIOGRAPHICITEM_SORT_COMPARE)
                            .forEachOrdered(bibliographicItem -> {

                                Instant modified = parseTimestamp(bibliographicItem.getModificationTimeStamp());
                                String bibliographicRecordId = bibliographicItem.getBibliographicRecordId();
                                try (LogWith logWith = new LogWith()) {
                                    logWith.bibliographicRecordId(bibliographicRecordId);
                                    BibliographicItemEntity bibItem = dao.getRecordCollection(bibliographicRecordId, getAgencyId(), modified);
                                    if (!bibItem.getModified().isAfter(modified)) {
                                        String note = orEmptyString(bibliographicItem.getNote());
                                        bibItem.setNote(note);
                                        bibItem.setModified(modified);
                                        bibItem.setTrackingId(getTrakingId());
                                    }

                                    bibliographicItem.getHoldings().stream()
                                            .sorted(HOLDINGS_SORT_COMPARE)
                                            .forEachOrdered(holding -> processHolding(modified, bibItem, holding, null));
                                    log.debug("bibItem = {}", bibItem);
                                    bibItem.save();
                                    touchedBibliographicRecordId(bibliographicRecordId);
                                }
                            });
                }
            });
        }
    }

    /**
     * Request for resetting one collection
     *
     * @param req soap request
     * @return soap response
     */
    @Timed
    public HoldingsItemsUpdateResult completeHoldingsItemsUpdate(final CompleteHoldingsItemsUpdateRequest req) {
        requestCompleteCounter.inc();
        if (req.getTrackingId() == null || req.getTrackingId().isEmpty()) {
            String trackingId = UUID.randomUUID().toString();
            log.info("Setting tracking id to: {}", trackingId);
            req.setTrackingId(trackingId);
        }
        try (LogWith logWith = new LogWith(req.getTrackingId())) {
            logWith.agencyId(req.getAgencyId());

            logXml(req.getAgencyId(), req, req.getAuthentication());
            return handleRequest(new UpdateRequest(this) {

                @Override
                public Authentication getAuthentication() {
                    return req.getAuthentication();
                }

                @Override
                public int getAgencyId() {
                    return req.getAgencyId();
                }

                @Override
                public String getTrakingId() {
                    return req.getTrackingId();
                }

                @Override
                public Optional<String> getQueueSupplierName() {
                    return config.getCompleteSupplier();
                }

                @Override
                public Optional<String> getOriginalQueueSupplierName() {
                    return config.getCompleteOriginalSupplier();
                }

                /**
                 * Wipe existing knowledge of record and create one from scratch
                 */
                @Override
                public void processBibliograhicItems() {
                    log.debug("complete");
                    CompleteBibliographicItem bibliographicItem = req.getCompleteBibliographicItem();
                    Instant modified = parseTimestamp(bibliographicItem.getModificationTimeStamp());
                    String bibliographicRecordId = bibliographicItem.getBibliographicRecordId();
                    try (LogWith logWith = new LogWith()) {
                        logWith.bibliographicRecordId(bibliographicRecordId);
                        BibliographicItemEntity bibItem = dao.getRecordCollection(bibliographicRecordId, getAgencyId(), modified);

                        if (!bibItem.getModified().isAfter(modified)) {
                            String note = orEmptyString(bibliographicItem.getNote());
                            bibItem.setNote(note);
                            bibItem.setModified(modified);
                            bibItem.setTrackingId(getTrakingId());
                        }

                        HashMap<String, Set<String>> processIssuesAndItems = new HashMap<>();
                        bibliographicItem.getHoldings().stream()
                                .sorted(HOLDINGS_SORT_COMPARE)
                                .forEachOrdered(holding -> {
                                    processHolding(modified, bibItem, holding, processIssuesAndItems);
                                });
                        Map<String, StateChangeMetadata> statuses = oldItemStatus.computeIfAbsent(bibliographicRecordId, f -> new HashMap<>());

                        bibItem.stream().forEach(issue -> {
                            if (issue.getComplete().isBefore(modified)) {
                                issue.setComplete(modified);
                            }
                            String issueId = issue.getIssueId();
                            log.debug("issueId = {}", issueId);
                            Set<String> processedItems = processIssuesAndItems.getOrDefault(issueId, EMPTY_SET);
                            List<ItemEntity> decommissioned = issue.stream() // For all items
                                    .filter(item -> !processedItems.contains(item.getItemId())) // Not those present in request
                                    .filter(item -> item.getStatus() != Status.ONLINE) // That aren't online type
                                    .filter(item -> !item.getModified().isAfter(modified)) // And haven't been modified in the future
                                    .collect(toList());
                            log.debug("decommissioned = {}", decommissioned);
                            decommissioned.forEach(item -> {
                                statuses.computeIfAbsent(item.getItemId(), i -> new StateChangeMetadata(item.getStatus(), item.getModified()))
                                        .update(Status.DECOMMISSIONED, modified);
                                item.remove();
                            });
                        });
                        bibItem.save();
                        touchedBibliographicRecordId(bibliographicRecordId);
                    }
                }
            });
        }
    }

    /**
     * Request for handling holdings on online resources
     * <p>
     * The issueId for en online holding is an empty string, so is the itemId
     * also. This itemId is not valid for anything else
     *
     * @param req soap request
     * @return soap response
     */
    @Timed
    public HoldingsItemsUpdateResult onlineHoldingsItemsUpdate(final OnlineHoldingsItemsUpdateRequest req) {
        requestOnlineCounter.inc();
        if (req.getTrackingId() == null || req.getTrackingId().isEmpty()) {
            String trackingId = UUID.randomUUID().toString();
            log.info("Setting tracking id to: {}", trackingId);
            req.setTrackingId(trackingId);
        }
        try (LogWith logWith = new LogWith(req.getTrackingId())) {
            logWith.agencyId(req.getAgencyId());

            logXml(req.getAgencyId(), req, req.getAuthentication());
            return handleRequest(new UpdateRequest(this) {

                @Override
                public Authentication getAuthentication() {
                    return req.getAuthentication();
                }

                @Override
                public int getAgencyId() {
                    return req.getAgencyId();
                }

                @Override
                public String getTrakingId() {
                    return req.getTrackingId();
                }

                @Override
                public Optional<String> getQueueSupplierName() {
                    return config.getOnlineSupplier();
                }

                @Override
                public Optional<String> getOriginalQueueSupplierName() {
                    return config.getOnlineOriginalSupplier();
                }

                @Override
                public void processBibliograhicItems() {
                    req.getOnlineBibliographicItems().stream()
                            .sorted(ONLINE_BIBLIOGRAPHICITEM_SORT_COMPARE)
                            .forEachOrdered(this::processBibliograhicItem);
                }

                /**
                 * Create items with issueId and itemId as empty strings
                 *
                 * @param bibliographicItem soap structure
                 */
                private void processBibliograhicItem(OnlineBibliographicItem bibliographicItem) {
                    Instant modified = parseTimestamp(bibliographicItem.getModificationTimeStamp());
                    String bibliographicRecordId = bibliographicItem.getBibliographicRecordId();
                    try (LogWith logWith = new LogWith()) {
                        logWith.bibliographicRecordId(bibliographicRecordId);
                        log.info("OnlineItem");
                        IssueEntity collection;
                        try (Timer.Context time = loadCollectionTimer.time()) {
                            collection = dao.getRecordCollection(bibliographicRecordId, getAgencyId(), modified)
                                    .issue("", modified);
                        }
                        collection
                                .setIssueText("ONLINE")
                                .setTrackingId(getTrakingId());
                        ItemEntity rec = collection.item("", modified); // Empty issueId = ONLINE
                        if (bibliographicItem.isHasOnlineHolding()) {
                            rec.setStatus(Status.ONLINE);
                        } else {
                            rec.remove();
                        }
                        if (rec.isNew()) {
                            rec.setBranch("");
                            rec.setBranchId("");
                            rec.setDepartment("");
                            rec.setLocation("");
                            rec.setSubLocation("");
                            rec.setCirculationRule("");
                            rec.setAccessionDate(LocalDate.now());
                        }
                        rec.setTrackingId(getTrakingId());
                        touchedBibliographicRecordId(bibliographicRecordId);
                        saveCollection(collection, modified);
                    } catch (HoldingsItemsException ex) {
                        throw new WrapperException(ex);
                    }
                }
            });
        }
    }

    // CPD-ON
    private void logXml(int agencyId, Object req, Authentication auth) {
        if (config.shouldLogXml(agencyId) || config.shouldLogXml(-1)) {
            if (auth != null) {
                String password = auth.getPasswordAut();
                auth.setPasswordAut("[REDACTED]");
                log.info(mapToXml.apply(req));
                auth.setPasswordAut(password);
            } else {
                log.info(mapToXml.apply(req));
            }
        }
    }

    /**
     * Process a request, with the specifics of that type
     *
     * @param req request object explaining the request type
     * @return soap response
     */
    private HoldingsItemsUpdateResult handleRequest(UpdateRequest req) {
        requestCounter.inc();
        try {
            soapValidation();
        } catch (SAXParseException ex) {
            requestInvalidCounter.inc();
            log.error("Soap Error: " + ex.getMessage());
            log.debug("Soap Error:", ex);
            return buildReponse(HoldingsItemsUpdateStatusEnum.FAILED_UPDATE_INTERNAL_ERROR, "Soap Error: " + ex.getMessage());
        }
        try {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, req.getTrakingId());
            req.setDao(dao);
            userValidation(req);
            try {
                req.processBibliograhicItems();
                req.queue();
            } catch (WrapperException ex) {
                ex.rethrow();
            }
            return buildReponse(HoldingsItemsUpdateStatusEnum.OK, "Success");
        } catch (HoldingsItemsException ex) {
            requestSystemErrorCounter.inc();
            log.error("HoldingsItemdsDAO exception: " + ex.getMessage());
            log.debug("HoldingsItemdsDAO exception:", ex);
            return buildReponse(HoldingsItemsUpdateStatusEnum.FAILED_UPDATE_INTERNAL_ERROR, "Database Error");
        } catch (SQLException ex) {
            requestSystemErrorCounter.inc();
            log.error("SQL exception: " + ex.getMessage());
            log.debug("SQL exception:", ex);
            return buildReponse(HoldingsItemsUpdateStatusEnum.FAILED_UPDATE_INTERNAL_ERROR, "Database Error");
        } catch (AuthenticationException ex) {
            requestAuthenticationErrorCounter.inc();
            log.error("Authentication failed: " + ex.getMessage());
            log.debug("Authentication failed:", ex);
            return buildReponse(HoldingsItemsUpdateStatusEnum.AUTHENTICATION_ERROR, "Authentication failed");
        } catch (FailedUpdateInternalException | InvalidDeliveryDateException ex) {
            requestSystemErrorCounter.inc();
            log.error("Update Internal exception: " + ex.getMessage());
            log.debug("Update Internal exception:", ex);
            return buildReponse(HoldingsItemsUpdateStatusEnum.FAILED_UPDATE_INTERNAL_ERROR, ex.getMessage());
        } catch (Exception ex) {
            requestSystemErrorCounter.inc();
            log.error("Unknown exception: " + ex.getMessage());
            log.debug("Unknown exception:", ex);
            return buildReponse(HoldingsItemsUpdateStatusEnum.FAILED_UPDATE_INTERNAL_ERROR, "Internal Server Error");
        }
    }

    /**
     * Set an error in a soap response
     *
     * @param code    the error code
     * @param message the error message
     * @return a soap reponse
     */
    private HoldingsItemsUpdateResult buildReponse(HoldingsItemsUpdateStatusEnum code, String message) {
        HoldingsItemsUpdateResult res = new HoldingsItemsUpdateResult();
        res.setHoldingsItemsUpdateStatus(code);
        res.setHoldingsItemsUpdateStatusMessage(message);
        return res;
    }

    /**
     * Check for stored XML errors from soap parsing
     *
     * @throws SAXParseException in case of errors
     */
    private void soapValidation() throws SAXParseException {
        MessageContext mc = wsc.getMessageContext();
        soapError((SAXParseException) mc.get(WsdlValidationHandler.WARNING), "warning", false);
        soapError((SAXParseException) mc.get(WsdlValidationHandler.ERROR), "error", true);
        soapError((SAXParseException) mc.get(WsdlValidationHandler.FATAL), "fatal", true);
    }

    /**
     * Log ant raise an error in case of xml syntax problems
     *
     * @param ex    exception
     * @param type  level of error
     * @param fatal should this raise an exception
     * @throws SAXParseException if fatal is set raise exception
     */
    private void soapError(SAXParseException ex, String type, boolean fatal) throws SAXParseException {
        if (ex != null) {
            requestInvalidCounter.inc();
            log.warn("Parsing SOAP " + type + ": " + ex.getLocalizedMessage());
            if (fatal) {
                ex.setStackTrace(new StackTraceElement[0]); // Useless stacktrace, provides no information
                throw ex;
            }
        }
    }

    /**
     * Validate a user
     *
     * @param req the full request
     * @throws AuthenticationException       In case of bad access
     * @throws FailedUpdateInternalException in case of errors accessing the
     *                                       authentication service
     */
    void userValidation(UpdateRequest req) throws AuthenticationException, FailedUpdateInternalException {
        Authentication authentication = req.getAuthentication();
        if (authentication == null) {
            log.error("No authentication supplied. Agency requested modified: {}", req.getAgencyId());
            if (config.getDisableAuthentication()) {
                return;
            }
            throw new AuthenticationException("No authentication supplied");
        } else {
            String validatedAgencyId = validator.validate(authentication);
            // Not validated
            if (validatedAgencyId == null) {
                log.error("User not validated {}/{}/...", authentication.getUserIdAut(), authentication.getGroupIdAut());
                if (config.getDisableAuthentication()) {
                    return;
                }
                throw new AuthenticationException("User not validated");
            }
            // Validated verify agency match
            if (Integer.parseUnsignedInt(validatedAgencyId) != req.getAgencyId()) {
                log.error("User validation ({}), record update mismatch ({})", validatedAgencyId, req.getAgencyId());
                if (config.getDisableAuthentication()) {
                    return;
                }
                throw new AuthenticationException("User validation, record update mismatch");
            }
        }
    }

    /**
     * If is null make an empty string
     *
     * @param s optionally null string
     * @return non null string
     */
    private static String orEmptyString(String s) {
        if (s == null) {
            return "";
        }
        return s;
    }

}
