package dk.dbc.holdingsitems.content;

import dk.dbc.commons.persistence.JpaIntegrationTest;
import dk.dbc.commons.persistence.JpaTestEnvironment;
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

/**
 * Copied from the other modules. Dunno if this is the right way to do it...
 */
public class JpaBase extends JpaIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(JpaBase.class);

    protected static PGSimpleDataSource dataSource;
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
        dataSource = getDataSource("holdingsitems");
        DatabaseMigrator.migrate(dataSource);
        return new JpaTestEnvironment(dataSource, "holdingsItemsManual_PU");
    }
    @Before
    public void cleanTables() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE item CASCADE");
            stmt.execute("TRUNCATE issue CASCADE");
            stmt.execute("TRUNCATE bibliographicItem CASCADE");
            stmt.execute("TRUNCATE queue CASCADE");
            stmt.execute("TRUNCATE queue_error CASCADE");
        }
        env().getEntityManagerFactory().getCache().evictAll();
    }

    private static PGSimpleDataSource getDataSource(String databaseName) {
        String testPort = System.getProperty("postgresql.port");
        PGSimpleDataSource ds = new PGSimpleDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return setLogging(super.getConnection());
            }

            @Override
            public Connection getConnection(String user, String password) throws SQLException {
                return setLogging(super.getConnection(user, password));
            }

            private Connection setLogging(Connection connection) {
                try (PreparedStatement stmt = connection.prepareStatement("SET log_statement = 'all';")) {
                    stmt.execute();
                } catch (SQLException ex) {
                    log.warn("Cannot set logging: {}", ex.getMessage());
                    log.debug("Cannot set logging:", ex);
                }
                return connection;
            }
        };

        String userName = System.getProperty("user.name");
        if (testPort != null) {
            ds.setServerNames(new String[] {"localhost"} );
            ds.setDatabaseName(databaseName);
            ds.setUser(userName);
            ds.setPassword(userName);
            ds.setPortNumbers(new int[] {Integer.parseUnsignedInt(testPort)});
        } else {
            Map<String, String> env = System.getenv();
            ds.setUser(env.getOrDefault("PGUSER", userName));
            ds.setPassword(env.getOrDefault("PGPASSWORD", userName));
            ds.setServerNames(new String[] {env.getOrDefault("PGHOST", "localhost")});
            ds.setPortNumbers(new int[] {Integer.parseUnsignedInt(env.getOrDefault("PGPORT", "5432"))});
            ds.setDatabaseName(env.getOrDefault("PGDATABASE", userName));
        }
        return ds;
    }

}
