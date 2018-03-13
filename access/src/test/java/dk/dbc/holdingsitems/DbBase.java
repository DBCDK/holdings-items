/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items
 *
 * holdings-items is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems;

import dk.dbc.commons.testutils.postgres.connection.PostgresITDataSource;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class DbBase {

    private static final Logger log = LoggerFactory.getLogger(DbBase.class);

    protected PostgresITDataSource pg;
    protected  DataSource dataSource;

    @Before
    public void setUp() throws Exception {
        pg = new PostgresITDataSource("holdingsitems");
        dataSource = pg.getDataSource();
        try (Connection connection = dataSource.getConnection() ;
             Statement stmt = connection.createStatement()) {
            log.info("Clearing database");
            stmt.executeUpdate("DROP SCHEMA public CASCADE");
            stmt.executeUpdate("CREATE SCHEMA public");
        }
        DatabaseMigrator.migrate(dataSource);
    }
}
