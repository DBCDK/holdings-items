package dk.dbc.holdingsitems.facade;

import dk.dbc.oss.ns.holdingsitemsupdate.BibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteHoldingsItemsUpdate;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteHoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdate;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateResponse;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateResult;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateStatusEnum;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineBibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineHoldingsItemsUpdate;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineHoldingsItemsUpdateRequest;
import dk.dbc.soap.facade.service.AbstractSoapServletWithRestClient;
import dk.dbc.soap.facade.service.SharedInstances;
import dk.dbc.soap.facade.service.TimingRecorder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import jakarta.ws.rs.WebApplicationException;
import java.net.URI;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.stream.Collectors;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class HoldingItemsUpdateServlet extends AbstractSoapServletWithRestClient {

    private static final Logger log = LoggerFactory.getLogger(HoldingItemsUpdateServlet.class);

    private final URI baseUri;
    private final PrometheusMeterRegistry registry;
    private final Timer completeTimer;
    private final Timer updateTimer;
    private final Timer onlineTimer;
    private final Timer badRequestTimer;
    private final Counter requests;
    private final Counter failures;
    private final Counter soapSyntaxError;
    private final Counter badOperation;
    private final Counter completeFailures;
    private final Counter updateFailures;
    private final Counter onlineFailures;
    private final EnumMap<HoldingsItemsUpdateStatusEnum, Counter> responseErrors;
    private final EnumMap<Response.Status, Counter> httpErrors;

    public HoldingItemsUpdateServlet(HoldingsItemsFacade config, PrometheusMeterRegistry registry) throws JAXBException {
        super(config, "holdingsItemsUpdate.wsdl");
        this.registry = registry;
        this.baseUri = URI.create(config.target.endsWith("/") ? config.target : config.target + "/");
        this.completeTimer = registry.timer("request_timings", "type", "complete");
        this.updateTimer = registry.timer("request_timings", "type", "update");
        this.onlineTimer = registry.timer("request_timings", "type", "online");
        this.badRequestTimer = registry.timer("request_timings", "type", "bad_request");
        this.requests = registry.counter("requests");
        this.failures = registry.counter("failures");
        this.soapSyntaxError = registry.counter("errors", "type", "soap_request");
        this.badOperation = registry.counter("errors", "type", "bad_soap_operation");
        this.responseErrors = new EnumMap<>(HoldingsItemsUpdateStatusEnum.class);
        EnumSet.complementOf(EnumSet.of(HoldingsItemsUpdateStatusEnum.OK))
                .stream()
                .forEach(status -> this.responseErrors.put(status, registry.counter("errors", "type", status.toString().toLowerCase(Locale.ROOT))));
        this.httpErrors = new EnumMap<>(Response.Status.class);
        EnumSet.allOf(Response.Status.class)
                .stream()
                .filter(i -> i.getStatusCode() >= 300)
                .forEach(status -> this.httpErrors.put(status, createHttpErrorCounter(status)));
        this.completeFailures = registry.counter("method_error", "type", "complete");
        this.updateFailures = registry.counter("method_error", "type", "update");
        this.onlineFailures = registry.counter("method_error", "type", "online");
    }

    private Counter createHttpErrorCounter(Response.Status status) {
        return registry.counter("errors", "type", "http_" + status.toString().toLowerCase(Locale.ROOT).replace(' ', '_'));
    }

    @Override
    protected Object processRequest(String operation, Element element, TimingRecorder timingRecorder, String remoteIp) throws Exception {
        requests.increment();
        switch (operation) {
            case "completeHoldingsItemsUpdate":
                timingRecorder.set(completeTimer);
                try {
                    CompleteHoldingsItemsUpdateRequest req = unmarshall(element, CompleteHoldingsItemsUpdate.class)
                            .getCompleteHoldingsItemsUpdateRequest();
                    HoldingsItemsUpdateResponse resp = client.target(baseUri.resolve(operation))
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .header(HttpHeader.X_FORWARDED_FOR.asString(), remoteIp)
                            .post(Entity.json(req), HoldingsItemsUpdateResponse.class);
                    if (resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatus() != HoldingsItemsUpdateStatusEnum.OK) {
                        log.warn("completeHoldingsItemsUpdate ({}/{}) from: {} returned: {}/{}",
                                 req.getAgencyId(), req.getCompleteBibliographicItem().getBibliographicRecordId(),
                                 remoteIp,
                                 resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatus(),
                                 resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatusMessage());
                        log.debug("Request:\n{}", dealyedLog(element, SharedInstances::toXMLStringOrError));
                        failures.increment();
                        completeFailures.increment();
                        responseErrors.get(resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatus()).increment();
                    }
                    return resp;
                } catch (WebApplicationException ex) {
                    httpErrors.computeIfAbsent(ex.getResponse().getStatusInfo().toEnum(), this::createHttpErrorCounter)
                            .increment();
                    throw ex;
                }
            case "holdingsItemsUpdate":
                timingRecorder.set(updateTimer);
                try {
                    HoldingsItemsUpdateRequest req = unmarshall(element, HoldingsItemsUpdate.class)
                            .getHoldingsItemsUpdateRequest();
                    HoldingsItemsUpdateResponse resp = client.target(baseUri.resolve(operation))
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .header(HttpHeader.X_FORWARDED_FOR.asString(), remoteIp)
                            .post(Entity.json(req), HoldingsItemsUpdateResponse.class);
                    if (resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatus() != HoldingsItemsUpdateStatusEnum.OK) {
                        log.warn("holdingsItemsUpdate ({}/{}) from: {} returned: {}/{}",
                                 req.getAgencyId(), req.getBibliographicItem().stream().map(BibliographicItem::getBibliographicRecordId).collect(Collectors.joining(",")),
                                 remoteIp,
                                 resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatus(),
                                 resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatusMessage());
                        log.debug("Request:\n{}", dealyedLog(element, SharedInstances::toXMLStringOrError));
                        failures.increment();
                        updateFailures.increment();
                        responseErrors.get(resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatus()).increment();
                    }
                    return resp;
                } catch (WebApplicationException ex) {
                    httpErrors.computeIfAbsent(ex.getResponse().getStatusInfo().toEnum(), this::createHttpErrorCounter)
                            .increment();
                    throw ex;
                }
            case "onlineHoldingsItemsUpdate":
                timingRecorder.set(onlineTimer);
                try {
                    OnlineHoldingsItemsUpdateRequest req = unmarshall(element, OnlineHoldingsItemsUpdate.class)
                            .getOnlineHoldingsItemsUpdateRequest();
                    HoldingsItemsUpdateResponse resp = client.target(baseUri.resolve(operation))
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .header(HttpHeader.X_FORWARDED_FOR.asString(), remoteIp)
                            .post(Entity.json(req), HoldingsItemsUpdateResponse.class);
                    if (resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatus() != HoldingsItemsUpdateStatusEnum.OK) {
                        log.warn("onlineHoldingsItemsUpdate ({}/{}) from: {} returned: {}/{}",
                                 req.getAgencyId(), req.getOnlineBibliographicItem().stream().map(OnlineBibliographicItem::getBibliographicRecordId).collect(Collectors.joining(",")),
                                 remoteIp,
                                 resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatus(),
                                 resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatusMessage());
                        log.debug("Request:\n{}", dealyedLog(element, SharedInstances::toXMLStringOrError));
                        failures.increment();
                        onlineFailures.increment();
                        responseErrors.get(resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatus()).increment();
                    }
                    return resp;
                } catch (WebApplicationException ex) {
                    httpErrors.computeIfAbsent(ex.getResponse().getStatusInfo().toEnum(), this::createHttpErrorCounter)
                            .increment();
                    throw ex;
                }
            default:
                timingRecorder.set(badRequestTimer);
                failures.increment();
                badOperation.increment();
                throw new UnsupportedOperationException("Operation not implemented");
        }
    }

    @Override
    protected Object processError(String operation, String error, TimingRecorder timingRecorder, String remoteIp) throws Exception {
        timingRecorder.set(badRequestTimer);
        soapSyntaxError.increment();
        failures.increment();
        log.warn("Got a faulty request to: {} from: {}, reason: {}", operation, remoteIp, error);
        HoldingsItemsUpdateResponse resp = new HoldingsItemsUpdateResponse();
        HoldingsItemsUpdateResult result = new HoldingsItemsUpdateResult();
        result.setHoldingsItemsUpdateStatus(HoldingsItemsUpdateStatusEnum.FAILED_UPDATE_INTERNAL_ERROR);
        result.setHoldingsItemsUpdateStatusMessage(error);
        resp.setHoldingsItemsUpdateResult(result);
        return resp;
    }
}
