/*
 * dbc-holdings-items-update-webservice
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043*
 *
 * This file is part of dbc-holdings-items-update-webservice.
 *
 * dbc-holdings-items-update-webservice is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * dbc-holdings-items-update-webservice is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with dbc-holdings-items-update-webservice.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.update;

import dk.dbc.forsrights.client.ForsRights;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Singleton
public class AccessCache {

    @Inject
    Config config;

    /**
     * Instance global cache
     */
    public ForsRights.RightsCache cache;

    @PostConstruct
    public void setup() {
        cache = new ForsRights.RightsCache((long)config.getMaxAgeMinutes() * 60 * 1000);
    }

}
