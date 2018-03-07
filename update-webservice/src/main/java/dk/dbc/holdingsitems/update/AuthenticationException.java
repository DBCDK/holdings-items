/*
 * Copyright (C) 2017 DBC A/S (http://dbc.dk/)
 *
 * This is part of dbc-holdings-items-update-service
 *
 * dbc-holdings-items-update-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dbc-holdings-items-update-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.update;

/**
 *
 * Exception used for indicating authorization errors
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class AuthenticationException extends RuntimeException {

    private static final long serialVersionUID = -6630246235332282540L;

    public AuthenticationException(String message) {
        super(message);
    }

}
