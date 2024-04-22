package dk.dbc.holdingsitems.facade;

import dk.dbc.oss.ns.holdingsitemsupdate.CompleteHoldingsItemsUpdate;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdate;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateResponse;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateResult;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateStatusEnum;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineHoldingsItemsUpdate;
import dk.dbc.soap.facade.service.AbstractSoapServletWithRestClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.net.URI;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.JAXBException;
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

    public HoldingItemsUpdateServlet(HoldingsItemsFacade config, PrometheusMeterRegistry registry) throws JAXBException {
        super(config, "holdingsItemsUpdate.wsdl");
        this.baseUri = URI.create(config.target.endsWith("/") ? config.target : config.target + "/");
        this.completeTimer = registry.timer("complete");
        this.updateTimer = registry.timer("update");
        this.onlineTimer = registry.timer("online");
        this.requests = registry.counter("requests");
        this.badOperation = registry.counter("bad-operation");
        this.failures = registry.counter("failures");

    }

    @Override
    protected Object processRequest(String operation, Element element, String remoteIp) throws Exception {
        requests.increment();
        switch (operation) {
            case "completeHoldingsItemsUpdate":
                return completeTimer.recordCallable(() -> {
                    Object req = unmarshall(element, CompleteHoldingsItemsUpdate.class)
                            .getCompleteHoldingsItemsUpdateRequest();
                    return client.target(baseUri.resolve(operation))
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .header(HttpHeader.X_FORWARDED_FOR.asString(), remoteIp)
                            .post(Entity.json(req), HoldingsItemsUpdateResponse.class);
                });
            case "holdingsItemsUpdate":
                return updateTimer.recordCallable(() -> {
                    Object req = unmarshall(element, HoldingsItemsUpdate.class)
                            .getHoldingsItemsUpdateRequest();
                    return client.target(baseUri.resolve(operation))
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .header(HttpHeader.X_FORWARDED_FOR.asString(), remoteIp)
                            .post(Entity.json(req), HoldingsItemsUpdateResponse.class);
                });
            case "onlineHoldingsItemsUpdate":
                return onlineTimer.recordCallable(() -> {
                    Object req = unmarshall(element, OnlineHoldingsItemsUpdate.class)
                            .getOnlineHoldingsItemsUpdateRequest();
                    return client.target(baseUri.resolve(operation))
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .header(HttpHeader.X_FORWARDED_FOR.asString(), remoteIp)
                            .post(Entity.json(req), HoldingsItemsUpdateResponse.class);
                });
            default:
                badOperation.increment();
                throw new UnsupportedOperationException("Operation not implemented");
        }
    }

    @Override
    protected Object processError(String operation, String error, String remoteIp) throws Exception {
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
