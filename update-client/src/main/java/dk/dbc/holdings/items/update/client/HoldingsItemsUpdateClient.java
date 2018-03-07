package dk.dbc.holdings.items.update.client;

import com.codahale.metrics.MetricRegistry;
import com.sun.xml.ws.client.BindingProviderProperties;
import dk.dbc.oss.ns.holdingsitemsupdate.Authentication;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteHoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdatePortType;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateResult;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateServices;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateStatusEnum;
import java.net.URL;
import javax.xml.ws.BindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A client for the holdings items update service.
 * wraps WSDL auto generated code.
 * 
 * Is thread safe
 */
public class HoldingsItemsUpdateClient {
    
    private final static Logger log = LoggerFactory.getLogger(HoldingsItemsUpdateClient.class);
    
    private final HoldingsItemsUpdateServices service;
    private final String url;

    private int connectTimeout = 10000;
    private int requestTimeout = 60000;
    
    // Forsrights
    private String user;
    private String group;
    private String pass;
    
    private final Timer holdingsItemsUpdateTimer;
    private final Timer completeHoldingsItemsUpdateTimer;    

    public HoldingsItemsUpdateClient(String url) {
        URL wsdl = HoldingsItemsUpdateServices.class.getResource("/holdingsItemsUpdate.wsdl");
        service = new HoldingsItemsUpdateServices(wsdl);  
        this.url = url;
        holdingsItemsUpdateTimer = new Timer();
        completeHoldingsItemsUpdateTimer = new Timer();
        log.info("Initialized HoldingsItemsUpdateClient with URL '{}'", url);
    }
    
    /**
     * Configures connect timeout of client.
     * @param timeout
     * @return the configured HoldingsItemsUpdateClient
     */
    public HoldingsItemsUpdateClient withConnectTimeout(int timeout){
        connectTimeout = timeout;
        return this;
    }
    
    /**
     * Configures request timeout of client.
     * @param timeout
     * @return the configured HoldingsItemsUpdateClient
     */
    public HoldingsItemsUpdateClient withRequestTimeout(int timeout){
        requestTimeout = timeout;
        return this;
    }
    
    /**
     * Configures client to use a given MetricRegistry.
     * Will then use timers for service requests
     * 
     * @param metrics
     * @return the configured HoldingsItemsUpdateClient
     */
    public HoldingsItemsUpdateClient withMetricsRegistry(MetricRegistry metrics){
        holdingsItemsUpdateTimer.create(getClass().getCanonicalName() + ".holdingsItemsUpdate", metrics);
        completeHoldingsItemsUpdateTimer.create(getClass().getCanonicalName() + ".completeHoldingsItemsUpdate", metrics);
        return this;
    }
    
    /**
     * Configures client to use Forsrights authentication.
     * Provided triple must be in format: user:group:pass
     * @param forsrightsTriple
     * @return the configured HoldingsItemsUpdateClient
     */
    public HoldingsItemsUpdateClient withAuthentication(String forsrightsTriple) {
        
        if(forsrightsTriple == null || forsrightsTriple.isEmpty()) {                
            throw new IllegalArgumentException("Given triple must not be empty or null");
        }
        
        String[] triple = forsrightsTriple.split(":");
        if(triple.length != 3) {
            throw new IllegalArgumentException("Wrong length of forsrights triple. Not 3 long but " + triple.length);
        }
        this.user = triple[0];
        this.group = triple[1];
        this.pass = triple[2];
        return this;
    }

    /**
     * Performs an update request.
     * 
     * @param req
     * @return
     * @throws dk.dbc.holdings.items.update.client.HoldingsItemsUpdateClient.UpdateException 
     */
    public HoldingsItemsUpdateResult holdingsItemsUpdate(Request.UpdateRequest req) throws UpdateException{
        try(Timer t = holdingsItemsUpdateTimer.time()) {
            
            HoldingsItemsUpdateRequest request = new HoldingsItemsUpdateRequest();
            request.setAgencyId(req.getAgencyId());
            request.setAuthentication(getAuthentication());
            request.setTrackingId(req.getTrackingId());
            request.getBibliographicItem().addAll(req.getItems());

            HoldingsItemsUpdateResult result = getPort().holdingsItemsUpdate(request);
            if (result.getHoldingsItemsUpdateStatus() != HoldingsItemsUpdateStatusEnum.OK) {
                throw new UpdateException(result.getHoldingsItemsUpdateStatusMessage(), result);
            }
            return result;
        }
    }
    
    /**
     * Performs a complete update request.
     * 
     * @param req
     * @return
     * @throws dk.dbc.holdings.items.update.client.HoldingsItemsUpdateClient.UpdateException 
     */
    public HoldingsItemsUpdateResult completeHoldingsItemsUpdate(Request.CompleteUpdateRequest req) throws UpdateException{
        try(Timer t = completeHoldingsItemsUpdateTimer.time()) {
            
            CompleteHoldingsItemsUpdateRequest request = new CompleteHoldingsItemsUpdateRequest();
            request.setAgencyId(req.getAgencyId());
            request.setAuthentication(getAuthentication());
            request.setTrackingId(req.getTrackingId());
            request.setCompleteBibliographicItem(req.getItem());        

            HoldingsItemsUpdateResult result = getPort().completeHoldingsItemsUpdate(request);
            if (result.getHoldingsItemsUpdateStatus() != HoldingsItemsUpdateStatusEnum.OK) {
                throw new UpdateException(result.getHoldingsItemsUpdateStatusMessage(), result);
            }
            return result;

        }
    }
    
    /**
     * Get a port instance from service.
     * The service object is thread safe, but the port object is not.
     * Hence we create a port per request, which should be fast enough -
     * if its found that this is not the case, we've gotta pool.    
     * @return
     */
    private HoldingsItemsUpdatePortType getPort() {
        HoldingsItemsUpdatePortType port = service.getHoldingsItemsUpdatePort(); // This is thread safe
        BindingProvider bindingProvider = (BindingProvider) port;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        bindingProvider.getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, connectTimeout);
        bindingProvider.getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, requestTimeout);
        return port;
    }
    
    
    private Authentication getAuthentication() {
        if(user == null || user.isEmpty()) {
            return null;
        } else {
            Authentication auth = new Authentication();
            auth.setUserIdAut(user);
            auth.setGroupIdAut(group);
            auth.setPasswordAut(pass);
            return auth;
        }
    }
    
    /**
     * Wraps a codehale timer to allow for metrics registry to be null.
     * 
     * The wrappers purpose is just to allow for time and close to be called without
     * a nullpointer exception is thrown.
     */
    private static class Timer implements AutoCloseable {
        private com.codahale.metrics.Timer t;
        private com.codahale.metrics.Timer.Context time;
        
        public void create(String name, MetricRegistry metrics){
            if(metrics == null) {
                t = null;
            } else {
                t = metrics.timer(name);
            }
        }
        
        public Timer time(){
            if(t != null) {
                time = t.time();
            }
            return this;
        }

        @Override
        public void close() {
            if(time != null) {
                time.close();
            }
        }                
    }
    
    public static class UpdateException extends Exception {
        private final HoldingsItemsUpdateResult result;
        
        public UpdateException(String message, HoldingsItemsUpdateResult result) {
            super(message);
            this.result = result;
        }
        
        /**
         * @return the result
         */
        public HoldingsItemsUpdateResult getResult() {
            return result;
        }
    }
}
