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
package dk.dbc.holdingsitems.purge;

import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.holdingsitems.DatabaseMigrator;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.LoanRestriction;
import dk.dbc.holdingsitems.jpa.Status;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;

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
    public static interface DaoVoidExecution {

        public void execute(EntityManager em, HoldingsItemsDAO dao) throws Exception;
    }

    @FunctionalInterface
    public static interface DaoExecution<T extends Object> {

        public T execute(EntityManager em, HoldingsItemsDAO dao) throws Exception;
    }

    public void exec(DaoVoidExecution exe) {
        JpaTestEnvironment e = env();
        e.reset();
        EntityManager em = e.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            exe.execute(em, HoldingsItemsDAO.newInstance(em));
            transaction.commit();
        } catch (Exception ex) {
            transaction.rollback();
            if (ex instanceof RuntimeException)
                throw (RuntimeException) ex;
            throw new RuntimeException(ex);
        }
    }

    public <T> T exec(DaoExecution<T> exe) {
        JpaTestEnvironment e = env();
        e.reset();
        EntityManager em = e.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            return exe.execute(em, HoldingsItemsDAO.newInstance(em));
        } catch (Exception ex) {
            transaction.rollback();
            if (ex instanceof RuntimeException)
                throw (RuntimeException) ex;
            throw new RuntimeException(ex);
        } finally {
            if (transaction.isActive())
                transaction.commit();
        }
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

    protected void insert(String... rows) {
        for (String s : rows) {
            exec((em, dao) -> {
                String[] parts = s.split("/");
                if (parts.length != 5)
                    throw new IllegalArgumentException("requires agencyid/bibliographicrecordid/issueid/itemid/status");
                Instant now = Instant.now();
                BibliographicItemEntity bib = dao.getRecordCollection(parts[1], Integer.parseUnsignedInt(parts[0]), now);
                IssueEntity issue = bib.issue(parts[2], now);
                issue.setIssueText("");
                issue.setUpdated(now);
                ItemEntity item = issue.item(parts[3], now);
                item.setStatus(Status.parse(parts[4]));
                item.setBranchId("x");
                item.setBranch("x");
                item.setDepartment("x");
                item.setLocation("x");
                item.setSubLocation("x");
                item.setCirculationRule("");
                item.setLoanRestriction(LoanRestriction.EMPTY);
                item.setAccessionDate(LocalDate.from(now.atZone(ZoneId.systemDefault())));
                bib.save();
            });
        }
    }

    protected void verify(String[]... sets) throws SQLException {
        HashSet<String> db = new HashSet<>();
        try (Connection connection = pg.createConnection() ;
             Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("SELECT agencyId, bibliographicRecordId, issueId, itemId, status FROM item")) {
            while (resultSet.next()) {
                int i = 0;
                int agencyId = resultSet.getInt(++i);
                String bibliographicRecordId = resultSet.getString(++i);
                String issueId = resultSet.getString(++i);
                String itemId = resultSet.getString(++i);
                Status status = Status.parse(resultSet.getString(++i));
                db.add(agencyId + "/" + bibliographicRecordId + "/" + issueId + "/" + itemId + "/" + status);
            }
        }
        System.out.println("db = " + db);
        boolean ok = true;
        for (String[] rows : sets) {
            for (String row : rows) {
                if (!db.remove(row)) {
                    System.err.println("Missing in db: " + row);
                    ok = false;
                }
            }
        }
        ok = ok && db.isEmpty();
        for (String row : db) {
            System.err.println("Extra   in db: " + row);
        }
        if (!ok)
            Assert.fail("Database content not as expected");
    }

    protected String[] list(String... a) {
        return a;
    }

    protected String[] concat(String[] a, String... b) {
        return Stream.concat(Stream.of(a), Stream.of(b))
                .toArray(String[]::new);
    }
}
