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

import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class DatabaseMigratorIT {

    @Rule
    public DBCPostgreSQLContainer pg = new DBCPostgreSQLContainer();

    @Test
    public void testMigrate() throws Exception {
        System.out.println("migrate");
        DataSource dataSource = pg.datasource();
        DatabaseMigrator.migrate(dataSource);
        try (Connection connection = dataSource.getConnection() ;
             Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("SELECT * FROM holdingsitems_status")) {
            if (resultSet.next()) {
                do {
                    String row = resultSet.getString(1);
                    System.out.println("row = " + row);
                } while (resultSet.next());
                return;
            }
        }
        Assert.fail("Expected rows");
    }

}
