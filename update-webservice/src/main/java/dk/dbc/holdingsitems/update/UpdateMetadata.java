/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of dbc-holdings-items-update-webservice
 *
 * dbc-holdings-items-update-webservice is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dbc-holdings-items-update-webservice is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.update;

import dk.dbc.oss.ns.holdingsitemsupdate.StatusType;
import java.sql.Timestamp;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class UpdateMetadata {

    private StatusType newStatus;
    private final StatusType oldStatus;
    private Timestamp when;

    public UpdateMetadata(Timestamp when) {
        this(null, null, when);
    }

    public UpdateMetadata(StatusType oldStatus, Timestamp when) {
        this(oldStatus, oldStatus, when);
    }

    public UpdateMetadata(StatusType newStatus, StatusType oldStatus, Timestamp when) {
        this.newStatus = newStatus;
        this.oldStatus = oldStatus;
        this.when = when;
    }

    public void update(StatusType status, Timestamp when) {
        this.newStatus = status;
        this.when = when;
    }

    public String getNewStatus() {
        return newStatus == null ? "UNKNOWN" : newStatus.value();
    }

    public String getOldStatus() {
        return oldStatus == null ? "UNKNOWN" : oldStatus.value();
    }

    public String getWhen() {
        return when.toInstant().toString();
    }
}
