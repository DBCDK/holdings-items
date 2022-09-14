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

import dk.dbc.commons.testcontainers.postgres.AbstractJpaTestBase;
import dk.dbc.holdingsitems.DatabaseMigrator;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.LoanRestriction;
import dk.dbc.holdingsitems.jpa.Status;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import javax.sql.DataSource;

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
    public void populateDatabase(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO queue_rules(supplier, consumer) VALUES('UPDATE', 'update')");
            stmt.execute("INSERT INTO queue_rules(supplier, consumer) VALUES('COMPLETE', 'complete')");
            stmt.execute("INSERT INTO queue_rules(supplier, consumer) VALUES('ONLINE', 'online')");
            stmt.execute("INSERT INTO queue_rules(supplier, consumer) VALUES('UPDATE_ORIGINAL', 'update-original')");
            stmt.execute("INSERT INTO queue_rules(supplier, consumer) VALUES('COMPLETE_ORIGINAL', 'complete-original')");
            stmt.execute("INSERT INTO queue_rules(supplier, consumer) VALUES('ONLINE_ORIGINAL', 'online-original')");
        }
    }

    @Override
    public Collection<String> keepContentOfTables() {
        return tablesWithMigratedContent();
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
