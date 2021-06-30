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
package dk.dbc.holdingsitems.kafkabridge;

import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.holdingsitems.DatabaseMigrator;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.LoanRestriction;
import dk.dbc.holdingsitems.jpa.Status;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import javax.persistence.EntityManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.persistence.config.PersistenceUnitProperties.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class JpaBase {

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
        e.reset();
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

    public JpaTestEnvironment env() {
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
                .setLoanRestriction(LoanRestriction.EMPTY)
                .setModified(Instant.now())
                .setCreated(Instant.now())
                .setTrackingId("Some-Id");
    }

}
