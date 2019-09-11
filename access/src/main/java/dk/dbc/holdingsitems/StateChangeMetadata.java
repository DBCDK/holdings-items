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

import dk.dbc.holdingsitems.jpa.HoldingsItemsStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class StateChangeMetadata {

    private HoldingsItemsStatus newStatus;
    private final HoldingsItemsStatus oldStatus;
    private Instant when;

    public StateChangeMetadata(HoldingsItemsStatus oldStatus, Instant when) {
        this(oldStatus, oldStatus, when);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public StateChangeMetadata(HoldingsItemsStatus newStatus, HoldingsItemsStatus oldStatus, Instant when) {
        this.newStatus = newStatus;
        this.oldStatus = oldStatus;
        this.when = when;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void update(HoldingsItemsStatus status, Instant when) {
        this.newStatus = status;
        this.when = when;
    }

    public String getNewStatus() {
        return newStatus == null ? "UNKNOWN" : newStatus.toString();
    }

    public String getOldStatus() {
        return oldStatus == null ? "UNKNOWN" : oldStatus.toString();
    }

    public String getWhen() {
        return when.toString();
    }

    @Override
    public String toString() {
        return "StateChangeMetadata{" + "newStatus=" + newStatus + ", oldStatus=" + oldStatus + ", when=" + when + '}';
    }
}
