package dk.dbc.holdingsitems.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Startup
public class ContentServiceConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ContentServiceConfiguration.class);
}