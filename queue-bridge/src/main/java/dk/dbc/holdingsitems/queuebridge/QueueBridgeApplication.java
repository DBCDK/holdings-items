/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of dbc-git:holdings-items-queue-bridge
 *
 * dbc-git:holdings-items-queue-bridge is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dbc-git:holdings-items-queue-bridge is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.queuebridge;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@ApplicationPath("/api")
public class QueueBridgeApplication extends Application {

    private static final Set<Class<?>> CLASSES = new HashSet<Class<?>>(Arrays.asList(
            Status.class
    ));

    @Override
    public Set<Class<?>> getClasses() {
        return CLASSES;
    }

}