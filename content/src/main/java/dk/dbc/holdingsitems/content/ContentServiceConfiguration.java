package dk.dbc.holdingsitems.content;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Startup
@Singleton
public class ContentServiceConfiguration {

    public static final String DATABASE = "jdbc/holdings-items";

}
