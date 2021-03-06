/*
 * Copyright (C) 2017-2018 DBC A/S (http://dbc.dk/)
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

/**
 * Raise any exception as a runtime exception
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class WrapperException extends RuntimeException {

    private static final long serialVersionUID = -4986504129300071335L;

    private final Exception ex;

    public WrapperException(Exception ex) {
        this.ex = ex;
    }

    void rethrow() throws Exception {
        throw ex;
    }

}
