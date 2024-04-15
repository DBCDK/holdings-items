package dk.dbc.holdingsitems.facade;

import dk.dbc.oss.ns.holdingsitemsupdate.CompleteHoldingsItemsUpdate;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdate;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateResponse;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateResult;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateStatusEnum;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineHoldingsItemsUpdate;
import dk.dbc.soap.facade.service.AbstractSoapServletWithRestClient;
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

    public HoldingItemsUpdateServlet(HoldingsItemsFacade config) throws JAXBException {
        super(config, "holdingsItemsUpdate.wsdl");
        this.baseUri = URI.create(config.target.endsWith("/") ? config.target : config.target + "/");
    }

    @Override
    protected Object processRequest(String operation, Element element, String remoteIp) throws Exception {
        switch (operation) {
            case "completeHoldingsItemsUpdate": {
                Object req = unmarshall(element, CompleteHoldingsItemsUpdate.class)
                        .getCompleteHoldingsItemsUpdateRequest();
                return client.target(baseUri.resolve(operation))
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .header(HttpHeader.X_FORWARDED_FOR.asString(), remoteIp)
                        .post(Entity.json(req), HoldingsItemsUpdateResponse.class);
            }
            case "holdingsItemsUpdate": {
                Object req = unmarshall(element, HoldingsItemsUpdate.class)
                        .getHoldingsItemsUpdateRequest();
                return client.target(baseUri.resolve(operation))
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .header(HttpHeader.X_FORWARDED_FOR.asString(), remoteIp)
                        .post(Entity.json(req), HoldingsItemsUpdateResponse.class);
            }
            case "onlineHoldingsItemsUpdate": {
                Object req = unmarshall(element, OnlineHoldingsItemsUpdate.class)
                        .getOnlineHoldingsItemsUpdateRequest();
                return client.target(baseUri.resolve(operation))
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .header(HttpHeader.X_FORWARDED_FOR.asString(), remoteIp)
                        .post(Entity.json(req), HoldingsItemsUpdateResponse.class);
            }
            default:
                throw new UnsupportedOperationException("Operation not implemented");
        }
    }

    @Override
    protected Object processError(String operation, String error, String remoteIp) throws Exception {
        log.warn("Got a faulty request to: {}, reason: {}", operation, error);
        HoldingsItemsUpdateResponse resp = new HoldingsItemsUpdateResponse();
        HoldingsItemsUpdateResult result = new HoldingsItemsUpdateResult();
        result.setHoldingsItemsUpdateStatus(HoldingsItemsUpdateStatusEnum.FAILED_UPDATE_INTERNAL_ERROR);
        result.setHoldingsItemsUpdateStatusMessage(error);
        resp.setHoldingsItemsUpdateResult(result);
        return resp;
    }
}
