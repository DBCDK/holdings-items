/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items-access
 *
 * holdings-items-access is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items-access is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.update;

import dk.dbc.commons.persistence.JpaIntegrationTest;
import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.holdingsitems.DatabaseMigrator;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.Status;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import javax.persistence.EntityManager;
import org.junit.Before;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
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
        EntityManager em = e.getEntityManager();
        e.getPersistenceContext().run(() -> {
            try {
                ex.execute(em);
            } catch (Exception exc) {
                log.error("Exception: {}", exc.getMessage());
                log.debug("Exception: ", exc);
                throw exc;
            }
        });
    }

    public <T> T jpa(JpaExecution<T> ex) {
        JpaTestEnvironment e = env();
        EntityManager em = e.getEntityManager();
        return e.getPersistenceContext().run(() -> {
            try {
                return ex.execute(em);
            } catch (Exception exc) {
                log.error("Exception: {}", exc.getMessage());
                log.debug("Exception: ", exc);
                throw exc;
            }
        });
    }

    public void flushAndEvict() {
        jpa(em -> {
            em.flush();
        });
        env().getEntityManagerFactory().getCache().evictAll();
    }

    @Override
    public JpaTestEnvironment setup() {
        dataSource = getDataSource("holdingsitems");
        DatabaseMigrator.migrate(dataSource);
        return new JpaTestEnvironment(dataSource, "holdingsItemsManual_PU");
    }

    @Before
    public void cleanTables() throws Exception {
        try (Connection connection = dataSource.getConnection() ;
             Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE item CASCADE");
            stmt.execute("TRUNCATE issue CASCADE");
            stmt.execute("TRUNCATE bibliographicItem CASCADE");
            stmt.execute("TRUNCATE queue CASCADE");
            stmt.execute("TRUNCATE queue_error CASCADE");
            stmt.execute("TRUNCATE q CASCADE");
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
            ds.setServerName("localhost");
            ds.setDatabaseName(databaseName);
            ds.setUser(userName);
            ds.setPassword(userName);
            ds.setPortNumber(Integer.parseUnsignedInt(testPort));
        } else {
            Map<String, String> env = System.getenv();
            ds.setUser(env.getOrDefault("PGUSER", userName));
            ds.setPassword(env.getOrDefault("PGPASSWORD", userName));
            ds.setServerName(env.getOrDefault("PGHOST", "localhost"));
            ds.setPortNumber(Integer.parseUnsignedInt(env.getOrDefault("PGPORT", "5432")));
            ds.setDatabaseName(env.getOrDefault("PGDATABASE", userName));
        }
        return ds;
    }

    protected BibliographicItemEntity fill(BibliographicItemEntity item) {
        Instant now = Instant.now();
        return item
                .setNote("Nothing")
                .setModified(now)
                .setTrackingId("Some-Id");
    }

    protected IssueEntity fill(IssueEntity collection) {
        Instant now = Instant.now();
        return collection
                .setIssueText("")
                .setExpectedDelivery(LocalDate.now().plusDays(1))
                .setReadyForLoan(1)
                .setComplete(now)
                .setModified(now)
                .setCreated(now)
                .setUpdated(now)
                .setTrackingId("Some-Id");
    }

    protected ItemEntity fill(ItemEntity item) {
        return item
                .setBranch("mybr")
                .setBranchId("123456")
                .setDepartment("dep")
                .setLocation("fiction")
                .setSubLocation("thriller")
                .setCirculationRule("")
                .setStatus(Status.ON_SHELF)
                .setAccessionDate(LocalDate.now().minusDays(1))
                .setLoanRestriction("")
                .setModified(Instant.now())
                .setCreated(Instant.now())
                .setTrackingId("Some-Id");
    }

}
