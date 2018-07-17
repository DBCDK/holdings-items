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

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.sun.xml.ws.developer.SchemaValidation;
import dk.dbc.ee.stats.Timed;
import dk.dbc.forsrights.client.ForsRightsException;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.Record;
import dk.dbc.holdingsitems.RecordCollection;
import dk.dbc.log.LogWith;
import dk.dbc.oss.ns.holdingsitemsupdate.Authentication;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteBibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteHoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateResult;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateStatusEnum;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineBibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineHoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.StatusType;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jws.WebService;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@WebService(serviceName = "HoldingsItemsUpdateServices",
            portName = "HoldingsItemsUpdatePort",
            endpointInterface = "dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdatePortType",
            targetNamespace = "http://oss.dbc.dk/ns/holdingsItemsUpdate",
            wsdlLocation = "WEB-INF/wsdl/holdingsItemsUpdate.wsdl")

@SchemaValidation(handler = WsdlValidationHandler.class, inbound = true, outbound = false)
public class UpdateWebservice {

    private static final Logger log = LoggerFactory.getLogger(UpdateWebservice.class);

    @Inject
    Config config;

    @Inject
    AccessValidator validator;

    @Resource(lookup = C.DATASOURCE)
    DataSource dataSource;

    @Resource
    WebServiceContext wsc;

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
                    dk.dbc.oss.ns.holdingsitemsupdate.Authentication.class,
                    dk.dbc.oss.ns.holdingsitemsupdate.BibliographicItem.class,
                    dk.dbc.oss.ns.holdingsitemsupdate.CompleteBibliographicItem.class,
                    dk.dbc.oss.ns.holdingsitemsupdate.CompleteHoldingsItemsUpdate.class,
                    dk.dbc.oss.ns.holdingsitemsupdate.CompleteHoldingsItemsUpdateRequest.class,
                    dk.dbc.oss.ns.holdingsitemsupdate.Holding.class,
                    dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItem.class,
                    dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdate.class,
                    dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateRequest.class,
                    dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateResponse.class,
                    dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateResult.class,
                    dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateStatusEnum.class,
                    dk.dbc.oss.ns.holdingsitemsupdate.ModificationTimeStamp.class,
                    dk.dbc.oss.ns.holdingsitemsupdate.OnlineBibliographicItem.class,
                    dk.dbc.oss.ns.holdingsitemsupdate.OnlineHoldingsItemsUpdate.class,
                    dk.dbc.oss.ns.holdingsitemsupdate.OnlineHoldingsItemsUpdateRequest.class,
                    dk.dbc.oss.ns.holdingsitemsupdate.StatusType.class);
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
                int agencyId = Integer.parseInt(req.getAgencyId(), 10);

                @Override
                public Authentication getAuthentication() {
                    return req.getAuthentication();
                }

                @Override
                public String getTrakingId() {
                    return req.getTrackingId();
                }

                @Override
                public String getQueueListOld() {
                    return config.getUpdateQueueOldList();
                }

                @Override
                public String getQueueList() {
                    return config.getUpdateQueueList();
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
                                Timestamp modified = parseTimestamp(bibliographicItem.getModificationTimeStamp());
                                String bibliographicRecordId = bibliographicItem.getBibliographicRecordId();
                                String note = orEmptyString(bibliographicItem.getNote());
                                try (LogWith logWith = new LogWith()) {
                                    logWith.bibliographicRecordId(bibliographicRecordId);

                                    addQueueJob(bibliographicRecordId, agencyId);

                                    bibliographicItem.getHoldings().stream()
                                            .sorted(HOLDINGS_SORT_COMPARE)
                                            .forEachOrdered(holding -> processHolding(modified, agencyId, bibliographicRecordId, note, false, holding));

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
                int agencyId = Integer.parseInt(req.getAgencyId(), 10);

                @Override
                public Authentication getAuthentication() {
                    return req.getAuthentication();
                }

                @Override
                public String getTrakingId() {
                    return req.getTrackingId();
                }

                @Override
                public String getQueueListOld() {
                    return config.getCompleteQueueOldList();
                }

                @Override
                public String getQueueList() {
                    return config.getCompleteQueueList();
                }

                /**
                 * Wipe existing knowledge of record and create one from scratch
                 */
                @Override
                public void processBibliograhicItems() {
                    log.debug("complete");
                    CompleteBibliographicItem bibliographicItem = req.getCompleteBibliographicItem();
                    Timestamp modified = parseTimestamp(bibliographicItem.getModificationTimeStamp());
                    String bibliographicRecordId = bibliographicItem.getBibliographicRecordId();
                    String note = orEmptyString(bibliographicItem.getNote());
                    try (LogWith logWith = new LogWith()) {
                        logWith.bibliographicRecordId(bibliographicRecordId);

                        addQueueJob(bibliographicRecordId, agencyId);
                        bibliographicItem.getHoldings().stream()
                                .sorted(HOLDINGS_SORT_COMPARE)
                                .forEachOrdered(holding -> processHolding(modified, agencyId, bibliographicRecordId, note, true, holding));
                        Set<String> handledIssues = bibliographicItem.getHoldings().stream().map(h -> h.getIssueId()).collect(Collectors.toSet());
                        decommissionExistingRecords(bibliographicRecordId, agencyId, modified, handledIssues);
                    }
                }

                /**
                 * Find all issueids and decommission them.
                 *
                 * @param bibliographicRecordId record id
                 * @param modified              then the request was made
                 * @throws WrapperException for holding exceptions through
                 *                          .stream()
                 */
                public void decommissionExistingRecords(String bibliographicRecordId, int agencyId, Timestamp modified, Set<String> handledIssues) throws WrapperException {
                    try (LogWith logWith = new LogWith()) {
                        logWith.bibliographicRecordId(bibliographicRecordId);
                        Set<String> issueIds = dao.getIssueIds(bibliographicRecordId, agencyId);
                        for (String issueId : issueIds) {
                            if (handledIssues.contains(issueId)) {
                                continue;
                            }
                            logWith.with("issueId", issueId);
                            log.info("Decommissioning");
                            log.debug("agencyId = " + agencyId + "; bibliographicRecordId = " + bibliographicRecordId + "; issueId = " + issueId + "; wipe");
                            RecordCollection collection;
                            try (Timer.Context time = loadCollectionTimer.time()) {
                                collection = dao.getRecordCollection(bibliographicRecordId, agencyId, issueId);
                            }
                            if (!collection.getCompleteTimestamp().after(modified)) {
                                decommissionEntireHolding(collection, modified);
                            } else {
                                log.info("Got older modified {} from last complete {}", modified, collection.getCompleteTimestamp());
                            }
                            log.debug("collection = {}", collection);
                            saveCollection(collection, modified);
                        }
                    } catch (HoldingsItemsException ex) {
                        throw new WrapperException(ex);
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
                int agencyId = Integer.parseInt(req.getAgencyId(), 10);

                @Override
                public Authentication getAuthentication() {
                    return req.getAuthentication();
                }

                @Override
                public String getTrakingId() {
                    return req.getTrackingId();
                }

                @Override
                public String getQueueListOld() {
                    return config.getOnlineQueueOldList();
                }

                @Override
                public String getQueueList() {
                    return config.getOnlineQueueList();
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
                    Timestamp modified = parseTimestamp(bibliographicItem.getModificationTimeStamp());
                    String bibliographicRecordId = bibliographicItem.getBibliographicRecordId();
                    try (LogWith logWith = new LogWith()) {
                        logWith.bibliographicRecordId(bibliographicRecordId);
                        log.info("OnlineItem");
                        RecordCollection collection;
                        try (Timer.Context time = loadCollectionTimer.time()) {
                            collection = dao.getRecordCollection(bibliographicRecordId, agencyId, "");
                        }
                        if (collection.isOriginal()) {
                            collection.setNote("");
                            collection.setReadyForLoan(0);
                        }
                        Record rec = collection.findRecord(""); // Empty issueId = ONLINE
                        if (bibliographicItem.isHasOnlineHolding()) {
                            rec.setStatus(StatusType.ONLINE.value());
                        } else {
                            rec.setStatus(StatusType.DECOMMISSIONED.value());
                        }
                        if (rec.isOriginal()) {
                            rec.setBranch("");
                            rec.setDepartment("");
                            rec.setLocation("");
                            rec.setSubLocation("");
                            rec.setCirculationRule("");
                            rec.setAccessionDate(new Date());
                        }
                        addQueueJob(bibliographicRecordId, agencyId);
                        saveCollection(collection, modified);
                    } catch (HoldingsItemsException ex) {
                        throw new WrapperException(ex);
                    }
                }
            });
        }
    }

    private void logXml(String agencyId, Object req, Authentication auth) {
        if (config.shouldLogXml(agencyId)) {
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
        try (Connection connection = getUTCConnection()) {
            HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(connection, req.getTrakingId());
            req.setDao(dao);
            userValidation(req.getAuthentication());
            try {
                req.processBibliograhicItems();
                req.queue();
            } catch (WrapperException ex) {
                ex.rethrow();
            }
            connection.commit();
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
     * Aquire a connection to the database with UTC time zone
     *
     * @return new connection in transaction mode
     * @throws SQLException in case of access error
     */
    public Connection getUTCConnection() throws SQLException {
        Connection connection = dataSource.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement("SET TIME ZONE 'UTC'")) {
            stmt.executeUpdate();
        }
        connection.setAutoCommit(false);
        return connection;
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
     * @param authentication soap authentication element
     * @throws AuthenticationException       In case of bad access
     * @throws FailedUpdateInternalException in case of errors accessing the
     *                                       authentication service
     */
    private void userValidation(Authentication authentication) throws AuthenticationException, FailedUpdateInternalException {
        if (config.getDisableAuthentication()) {
            return;
        }
        MessageContext mc = wsc.getMessageContext();
        HttpServletRequest req = (HttpServletRequest) mc.get(MessageContext.SERVLET_REQUEST);
        String ip = getIp(req);
        try {
            if (!validator.validate(authentication, ip, config.getRightsGroup(), config.getRightsName())) {
                String authText = "[Unknown]";
                if (authentication != null) {
                    authText = authentication.getUserIdAut() + "/" + authentication.getGroupIdAut();
                }
                log.error("Error validating {} from {}",
                          authText, ip
                );
                throw new AuthenticationException("Unauthorized");
            }
        } catch (ForsRightsException ex) {
            log.error("Cannot get rights: {}", ex.getMessage());
            throw new FailedUpdateInternalException("Authentication service unavailable");
        }
    }

    /**
     * Get an ip number for the remote user
     * <p>
     * x-forwarded-for header is used if thre remote ip is known
     *
     * @param req Servlet context
     * @return Ip number
     */
    private String getIp(HttpServletRequest req) {
        String ip = req.getRemoteAddr();
        if (config.getXForwardedFor().contains(ip)) {
            String x = req.getHeader("x-forwarded-for");
            if (x != null) {
                String[] split = x.split(", ");
                ip = split[split.length - 1];
            }
        }
        return ip;
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
