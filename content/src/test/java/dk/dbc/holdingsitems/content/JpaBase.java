package dk.dbc.holdingsitems.content;

import dk.dbc.commons.persistence.JpaIntegrationTest;
import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.holdingsitems.DatabaseMigrator;
import org.junit.Before;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;

/**
 * Copied from the other modules. Dunno if this is the right way to do it...
 */
public class JpaBase extends JpaIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(JpaBase.class);

    @ClassRule
    public static DBCPostgreSQLContainer pg = new DBCPostgreSQLContainer();
    private static Map<String, String> emProperties;

    @BeforeClass
    public static void migrateTestPg() {
        DatabaseMigrator.migrate(pg.datasource());
        emProperties = Map.of(JDBC_USER, pg.getUsername(),
                              JDBC_PASSWORD, pg.getPassword(),
                              JDBC_URL, pg.getJdbcUrl(),
                              JDBC_DRIVER, "org.postgresql.Driver",
                              "eclipselink.logging.level", "FINE");
    }

    @FunctionalInterface
    public static interface JpaVoidExecution {

        public void execute(EntityManager em) throws Exception;
    }

    @FunctionalInterface
    public static interface JpaExecution<T extends Object> {

        public T execute(EntityManager em) throws Exception;
    }

    public void jpa(JpaVoidExecution ex) {
        JpaTestEnvironment e = env();
        e.reset();
        EntityManager em = e.getEntityManager();
        e.getPersistenceContext().run(() -> ex.execute(em));
    }

    public <T> T jpa(JpaExecution<T> ex) {
        JpaTestEnvironment e = env();
        e.reset();
        EntityManager em = e.getEntityManager();
        return e.getPersistenceContext().run(() -> ex.execute(em));
    }

    @Override
    public JpaTestEnvironment setup() {
        return new JpaTestEnvironment(pg.datasource(), "holdingsItemsManual_PU", emProperties);
    }

    @Before
    public void cleanTables() throws Exception {
        try (Connection connection = pg.createConnection() ;
             Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE item CASCADE");
            stmt.execute("TRUNCATE issue CASCADE");
            stmt.execute("TRUNCATE bibliographicItem CASCADE");
            stmt.execute("TRUNCATE queue CASCADE");
            stmt.execute("TRUNCATE queue_error CASCADE");
        }
        env().getEntityManagerFactory().getCache().evictAll();
    }
}
