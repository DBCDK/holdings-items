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
import java.util.stream.Collectors;

/**
 * @author Noah Torp-Smith (nots@dbc.dk)
 */
public enum LoanRestriction {
    a("a"),
    b("b"),
    c("c"),
    d("d"),
    e("e"),
    f("f"),
    g("g"),
    EMPTY("");

    private final String name;

    LoanRestriction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    private static final Map<String, LoanRestriction> LOOKUP =
        EnumSet.allOf(LoanRestriction.class)
                .stream()
                .collect(Collectors.toMap(e -> e.toString().toLowerCase(Locale.ROOT)
                , e -> e));

    public static LoanRestriction parse(String value) {
        if (value == null) {
            return null;
        }
        LoanRestriction res = LOOKUP.get(value);
        if (res == null) {
            throw new IllegalArgumentException("Cannot parse string " + value + " into a holdingsItems loanRestriction");
        }
        return res;
    }
}
