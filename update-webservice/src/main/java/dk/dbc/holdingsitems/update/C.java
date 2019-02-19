/*
 * Copyright (C) 2016-2018 DBC A/S (http://dbc.dk/)
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
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class C {

    public static final String PROPERTIES_LOOKUP = "holdingsitems-update";

    public static final String UPDATE_QUEUE_LIST = "UPDATE_QUEUE_LIST";
    public static final String UPDATE_QUEUE_LIST_DEFAULT = "update-worker";

    public static final String COMPLETE_QUEUE_LIST = "COMPLETE_QUEUE_LIST";
    public static final String COMPLETE_QUEUE_LIST_DEFAULT = "complete-worker";

    public static final String ONLINE_QUEUE_LIST = "ONLINE_QUEUE_LIST";
    public static final String ONLINE_QUEUE_LIST_DEFAULT = "online-worker";

    public static final String DISABLE_AUTHENTICATION = "DISABLE_AUTHENTICATION";
    public static final String DISABLE_AUTHENTICATION_DEFAULT = "true";

    public static final String FORS_RIGHTS_URL = "FORS_RIGHTS_URL";

    public static final String MAX_AGE_MINUTES = "MAX_AGE_MINUTES";
    public static final String MAX_AGE_MINUTES_DEFAULT = "480"; // 8 Hours

    public static final String RIGHTS_GROUP = "RIGHTS_GROUP";
    public static final String RIGHTS_NAME = "RIGHTS_NAME";

    public static final String DEBUG_XML_AGENCIES = "DEBUG_XML_AGENCIES";
    public static final String DEBUG_XML_AGENCIES_DEFAULT = "";

    public static final String DATASOURCE = "jdbc/holdingsitemsupdate/holdingsitems";

}
