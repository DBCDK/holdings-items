/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items-purge-tool
 *
 * holdings-items-purge-tool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items-purge-tool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.purge;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ExitException extends Exception {

    private static final long serialVersionUID = -3060411477487720821L;

    private final int status;

    public ExitException(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

}
