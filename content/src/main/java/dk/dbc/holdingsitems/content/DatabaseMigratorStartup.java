package dk.dbc.holdingsitems.content;

import dk.dbc.holdingsitems.DatabaseMigrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import javax.sql.DataSource;

@Singleton
@Startup
public class DatabaseMigratorStartup {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigratorStartup.class);

    @Resource(lookup = "jdbc/holdings-items")
    DataSource dataSource;

    @PostConstruct
    public void init() {
        try {
            DatabaseMigrator.migrate(dataSource);
        } catch (RuntimeException ex) {
            log.error("Error migrating database: {}", ex.getMessage());
            log.debug("Error migrating database: ", ex);
            throw new EJBException("CANNOT START UP - DATABASE ERROR");
        }
    }

}
