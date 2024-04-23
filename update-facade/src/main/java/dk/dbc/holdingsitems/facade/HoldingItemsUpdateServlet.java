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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.net.URI;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.JAXBException;
import java.util.stream.Collectors;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class HoldingItemsUpdateServlet extends AbstractSoapServletWithRestClient {

    private static final Logger log = LoggerFactory.getLogger(HoldingItemsUpdateServlet.class);

    private final URI baseUri;
    private final Timer completeTimer;
    private final Timer updateTimer;
    private final Timer onlineTimer;
    private final Counter requests;
    private final Counter badOperation;
    private final Counter failures;
    private final Counter soapFailures;
    private final Counter completeFailures;
    private final Counter updateFailures;
    private final Counter onlineFailures;

    public HoldingItemsUpdateServlet(HoldingsItemsFacade config, PrometheusMeterRegistry registry) throws JAXBException {
        super(config, "holdingsItemsUpdate.wsdl");
        this.baseUri = URI.create(config.target.endsWith("/") ? config.target : config.target + "/");
        this.completeTimer = registry.timer("complete");
        this.updateTimer = registry.timer("update");
        this.onlineTimer = registry.timer("online");
        this.requests = registry.counter("requests");
        this.badOperation = registry.counter("bad-operation");
        this.failures = registry.counter("failures");
        this.soapFailures = registry.counter("soap-failures");
        this.completeFailures = registry.counter("complete-failures");
        this.updateFailures = registry.counter("update-failures");
        this.onlineFailures = registry.counter("online-failures");

    }

    @Override
    protected Object processRequest(String operation, Element element, String remoteIp) throws Exception {
        requests.increment();
        switch (operation) {
            case "completeHoldingsItemsUpdate":
                return completeTimer.recordCallable(() -> {
                    CompleteHoldingsItemsUpdateRequest req = unmarshall(element, CompleteHoldingsItemsUpdate.class)
                            .getCompleteHoldingsItemsUpdateRequest();
                    HoldingsItemsUpdateResponse resp = client.target(baseUri.resolve(operation))
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .header(HttpHeader.X_FORWARDED_FOR.asString(), remoteIp)
                            .post(Entity.json(req), HoldingsItemsUpdateResponse.class);
                    if (resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatus() != HoldingsItemsUpdateStatusEnum.OK) {
                        log.warn("completeHoldingsItemsUpdate ({}/{}) returned: {}/{}",
                                 req.getAgencyId(), req.getCompleteBibliographicItem().getBibliographicRecordId(),
                                 resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatus(),
                                 resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatusMessage());
                        failures.increment();
                        completeFailures.increment();
                    }
                    return resp;
                });
            case "holdingsItemsUpdate":
                return updateTimer.recordCallable(() -> {
                    HoldingsItemsUpdateRequest req = unmarshall(element, HoldingsItemsUpdate.class)
                            .getHoldingsItemsUpdateRequest();
                    HoldingsItemsUpdateResponse resp = client.target(baseUri.resolve(operation))
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .header(HttpHeader.X_FORWARDED_FOR.asString(), remoteIp)
                            .post(Entity.json(req), HoldingsItemsUpdateResponse.class);
                    if (resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatus() != HoldingsItemsUpdateStatusEnum.OK) {
                        log.warn("holdingsItemsUpdate ({}/{}) returned: {}/{}",
                                 req.getAgencyId(), req.getBibliographicItem().stream().map(BibliographicItem::getBibliographicRecordId).collect(Collectors.joining(",")),
                                 resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatus(),
                                 resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatusMessage());
                        failures.increment();
                        updateFailures.increment();
                    }
                    return resp;
                });
            case "onlineHoldingsItemsUpdate":
                return onlineTimer.recordCallable(() -> {
                    OnlineHoldingsItemsUpdateRequest req = unmarshall(element, OnlineHoldingsItemsUpdate.class)
                            .getOnlineHoldingsItemsUpdateRequest();
                    HoldingsItemsUpdateResponse resp = client.target(baseUri.resolve(operation))
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .header(HttpHeader.X_FORWARDED_FOR.asString(), remoteIp)
                            .post(Entity.json(req), HoldingsItemsUpdateResponse.class);
                    if (resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatus() != HoldingsItemsUpdateStatusEnum.OK) {
                        log.warn("onlineHoldingsItemsUpdate ({}/{}) returned: {}/{}",
                                 req.getAgencyId(), req.getOnlineBibliographicItem().stream().map(OnlineBibliographicItem::getBibliographicRecordId).collect(Collectors.joining(",")),
                                 resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatus(),
                                 resp.getHoldingsItemsUpdateResult().getHoldingsItemsUpdateStatusMessage());
                        failures.increment();
                        onlineFailures.increment();
                    }
                    return resp;
                });
            default:
                failures.increment();
                badOperation.increment();
                throw new UnsupportedOperationException("Operation not implemented");
        }
    }

    @Override
    protected Object processError(String operation, String error, String remoteIp) throws Exception {
        soapFailures.increment();
        failures.increment();
        log.warn("Got a faulty request to: {}, reason: {}", operation, error);
        HoldingsItemsUpdateResponse resp = new HoldingsItemsUpdateResponse();
        HoldingsItemsUpdateResult result = new HoldingsItemsUpdateResult();
        result.setHoldingsItemsUpdateStatus(HoldingsItemsUpdateStatusEnum.FAILED_UPDATE_INTERNAL_ERROR);
        result.setHoldingsItemsUpdateStatusMessage(error);
        resp.setHoldingsItemsUpdateResult(result);
        return resp;
    }
}
