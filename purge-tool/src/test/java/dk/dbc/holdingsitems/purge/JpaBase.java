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

import dk.dbc.commons.testcontainers.postgres.AbstractJpaTestBase;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import javax.sql.DataSource;
import org.junit.Assert;

import static dk.dbc.holdingsitems.jpa.TablesWithMigratedContent.tablesWithMigratedContent;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class JpaBase extends AbstractJpaTestBase {

    @Override
    public String persistenceUnitName() {
        return "holdingsItemsManual_PU";
    }

    @Override
    public void migrate(DataSource dataSource) {
        DatabaseMigrator.migrate(dataSource);
    }

    @Override
    public Collection<String> keepContentOfTables() {
        return tablesWithMigratedContent();
    }

    protected void insert(String... rows) {
        for (String s : rows) {
            jpa(em -> {
                HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em);
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
        try (Connection connection = PG.createConnection() ;
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
}
