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
package dk.dbc.holdingsitems.update;

import dk.dbc.holdingsitems.DatabaseMigrator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Singleton
@Startup
public class DatabaseMigratorStartup {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigratorStartup.class);

    @Resource(lookup = C.DATASOURCE)
    DataSource dataSource;

    @PostConstruct
    public void init() {
        try {
            DatabaseMigrator.migrate(dataSource);
        } catch (RuntimeException ex) {
            log.error("Error migrating database: {}", ex.getMessage());
            log.debug("Error migrating database: ", ex);
            throw new EJBException("CANNOT START UP - DATABASE ERROR");
        }
    }

}
