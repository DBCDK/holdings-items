package dk.dbc.holdingsitems.content;

import dk.dbc.commons.testcontainers.postgres.AbstractJpaAndRestTestBase;
import dk.dbc.holdingsitems.DatabaseMigrator;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import javax.sql.DataSource;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

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


    public static IssueEntity issueEntity(BibliographicItemEntity bibliographicItemEntity, String issueId) {
        IssueEntity issueEntity = bibliographicItemEntity.issue(issueId, Instant.now());
        issueEntity.setTrackingId("track");
        issueEntity.setIssueText("old#1");
        issueEntity.setReadyForLoan(1);
        return issueEntity;
    }

    public static ItemEntity itemEntity(IssueEntity issueEntity, String itemId, dk.dbc.holdingsitems.jpa.Status status) {
        ItemEntity itemEntity = issueEntity.item(itemId, Instant.now());
        itemEntity.setAccessionDate(LocalDate.now(ZoneOffset.UTC));
        itemEntity.setStatus(status);
        itemEntity.setBranch("branch");
        itemEntity.setBranchId("9876");
        itemEntity.setDepartment("department");
        itemEntity.setLocation("location");
        itemEntity.setSubLocation("subLocation");
        itemEntity.setCirculationRule("");
        itemEntity.setLastLoanDate(LocalDate.of(2020, 4, 16));
        return itemEntity;
    }

    public static <T, R> FieldMatcher field(String field, Matcher<R> matcher) {
        return new FieldMatcher(field, matcher);
    }

    public static class FieldMatcher<T, R> extends BaseMatcher<T> {

        private final String field;
        private final Matcher<R> matcher;
        private String error;

        private FieldMatcher(String field, Matcher<R> matcher) {
            this.field = field;
            this.matcher = matcher;
            this.error = null;
        }

        @Override
        public boolean matches(Object item) {
            if (item == null) {
                error = "object needs to be defined";
            } else {
                try {
                    Field f = item.getClass().getField(field);
                    return matcher.matches(f.get(item));
                } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
                    error = "cannot access field: '" + field + "'";
                }
            }
            return false;
        }

        @Override
        public void describeMismatch(Object item, Description mismatchDescription) {
            if (error == null) {
                mismatchDescription.appendText("." + field + " ");
                matcher.describeMismatch(item, mismatchDescription);
            } else {
                mismatchDescription.appendText(error);
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("." + field + " ");
            matcher.describeTo(description);
        }
    }
}
