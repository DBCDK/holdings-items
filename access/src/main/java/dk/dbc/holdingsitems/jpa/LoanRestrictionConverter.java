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

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Noah Torp-Smith (nots@dbc.dk)
 */
@Converter
public class LoanRestrictionConverter implements AttributeConverter<LoanRestriction, String> {
    @Override
    public String convertToDatabaseColumn(LoanRestriction loanRestriction) {
        return loanRestriction.toString().toLowerCase();
    }

    @Override
    public LoanRestriction convertToEntityAttribute(String s) {
        return LoanRestriction.parse(s);
    }
}
