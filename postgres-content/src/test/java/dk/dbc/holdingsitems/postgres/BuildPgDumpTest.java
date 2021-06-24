/*
 * Copyright (C) 2021 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items-postgres
 *
 * holdings-items-postgres is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items-postgres is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.postgres;

import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.holdingsitems.DatabaseMigrator;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class BuildPgDumpTest {

    @ClassRule
    public static DBCPostgreSQLContainer pg = new DBCPostgreSQLContainer();

    @BeforeClass
    public static void migrate() {
        DatabaseMigrator.migrate(pg.datasource());
    }

    @Test(timeout = 2_000L)
    public void dumpContent() throws Exception {
        URL resource = getClass().getClassLoader().getResource(".");
        System.out.println("resource = " + resource);
        File file = Path.of(resource.toURI()).toAbsolutePath().toFile();
        while (!file.getParentFile().equals(file) && !file.getName().equals("target")) {
            file = file.getParentFile();
        }
        file = file.toPath().resolve("holdingsitems.sql").toFile();
        System.out.println("Dumping to: " + file);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(pg.pgdump(true).getBytes(StandardCharsets.UTF_8));
        }
    }
}
