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
package dk.dbc.holdingsitems.jpa;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public enum Status {

    ON_ORDER("OnOrder"),
    NOT_FOR_LOAN("NotForLoan"),
    ON_LOAN("OnLoan"),
    ON_SHELF("OnShelf"),
    DECOMMISSIONED("Decommissioned"),
    ONLINE("Online"),
    UNKNOWN("UNKNOWN");

    private final String name;

    Status(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    private static final Map<String, Status> LOOKUP =
            EnumSet.allOf(Status.class)
                    .stream()
                    .collect(toMap(e -> e.toString().toLowerCase(Locale.ROOT),
                                   e -> e));

    public static Status parse(String value) {
        Status status = LOOKUP.get(value.toLowerCase(Locale.ROOT));
        if (status == null)
            throw new IllegalArgumentException("Cannot turn `" + value + "' into a holdingsitems.Status");
        return status;
    }
}
