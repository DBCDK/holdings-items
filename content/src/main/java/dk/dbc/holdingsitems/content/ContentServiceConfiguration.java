package dk.dbc.holdingsitems.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Startup
@Singleton
public class ContentServiceConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ContentServiceConfiguration.class);
    public static final String DATABASE = "jdbc/holdings-items";

}
