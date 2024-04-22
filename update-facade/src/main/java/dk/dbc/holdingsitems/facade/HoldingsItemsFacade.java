package dk.dbc.holdingsitems.facade;

import dk.dbc.soap.facade.service.AbstractSoapServiceWithRestClient;
import dk.dbc.soap.facade.service.AbstractSoapServletWithRestClient;
import dk.dbc.soap.facade.service.SoapMain;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import picocli.CommandLine;

public class HoldingsItemsFacade extends AbstractSoapServiceWithRestClient {

    @CommandLine.Option(names = {"--target-url", "-t"},
                        paramLabel = "URL",
                        required = true,
                        description = "Request url for the REST context root (without operation name)")
    public String target;

    @Override
    public AbstractSoapServletWithRestClient getServlet(PrometheusMeterRegistry registry) throws Exception {
        return new HoldingItemsUpdateServlet(this, registry);
    }

    public static void main(String[] args) {
        new SoapMain(new HoldingsItemsFacade()).run(args);
    }
}
