/*
 * dbc-holdings-items-access
 * Copyright (C) 2014 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of dbc-holdings-items-access.
 *
 * dbc-holdings-items-access is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dbc-holdings-items-access is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with dbc-holdings-items-access.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems;

import java.sql.Timestamp;
import java.util.Date;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public interface Record {

    String NotForLoan = "NotForLoan";
    String OnLoan = "OnLoan";
    String OnOrder = "OnOrder";
    String OnShelf = "OnShelf";
    String Online = "Online";
    String Decommissioned = "Decommissioned";

    String getBranch();

    void setBranch(String branch);

    String getCirculationRule();

    void setCirculationRule(String circulationRule);

    String getDepartment();

    void setDepartment(String department);

    String getItemId();

    String getLocation();

    void setLocation(String location);

    Date getAccessionDate();

    void setAccessionDate(Date accessionDate);

    String getStatus();

    void setStatus(String status);

    String getSubLocation();

    void setSubLocation(String subLocation);

    boolean isModified();

    boolean isOriginal();

    Timestamp getCreated();

    Timestamp getModified();

    String getTrackingId();

}
