package dk.dbc.holdingsitems.content;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Startup
@Singleton
public class ContentServiceConfiguration {

    public static final String DATABASE = "jdbc/holdings-items";

}
