/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items
 *
 * holdings-items is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems;

import java.sql.Timestamp;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class StateChangeMetadata {

    private String newStatus;
    private final String oldStatus;
    private Timestamp when;

    public StateChangeMetadata(Timestamp when) {
        this(null, null, when);
    }

    public StateChangeMetadata(String oldStatus, Timestamp when) {
        this(oldStatus, oldStatus, when);
    }

    public StateChangeMetadata(String newStatus, String oldStatus, Timestamp when) {
        this.newStatus = newStatus;
        this.oldStatus = oldStatus;
        this.when = when;
    }

    public void update(String status, Timestamp when) {
        this.newStatus = status;
        this.when = when;
    }

    public String getNewStatus() {
        return newStatus == null ? "UNKNOWN" : newStatus;
    }

    public String getOldStatus() {
        return oldStatus == null ? "UNKNOWN" : oldStatus;
    }

    public String getWhen() {
        return when.toInstant().toString();
    }
}
