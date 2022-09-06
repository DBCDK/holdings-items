package dk.dbc.holdingsitems.content;

import dk.dbc.commons.testcontainers.postgres.AbstractJpaAndRestTestBase;
import dk.dbc.holdingsitems.DatabaseMigrator;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import javax.sql.DataSource;

import static dk.dbc.holdingsitems.jpa.TablesWithMigratedContent.tablesWithMigratedContent;

public class JpaBase extends AbstractJpaAndRestTestBase {

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

    @Override
    public void populateDatabase(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO queue_rules(supplier, consumer) VALUES('SUPERSEDE', 'cons')");
        }
    }
}
