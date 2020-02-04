package dk.dbc.holdingsitems.content;

import dk.dbc.commons.testutils.postgres.connection.PostgresITDataSource;
import dk.dbc.holdingsitems.DatabaseMigrator;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;

public class ContentIT {
    private static final Logger log = LoggerFactory.getLogger(ContentIT.class);

    private PostgresITDataSource pg;
    private DataSource dataSource;

    @Before
    public void setup() throws Exception {
        pg = new PostgresITDataSource("holdingsitems");
        dataSource = pg.getDataSource();
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DROP SCHEMA public CASCADE");
            stmt.executeUpdate("CREATE SCHEMA public");
        }
        DatabaseMigrator.migrate(dataSource);
    }

    @Test(timeout = 30_000L)
    public void test() throws Exception {
        System.out.println("Dummy test!");
        try (Connection connection = dataSource.getConnection()) {
            assertEquals(100, 10 * 10);
        }
    }


}
