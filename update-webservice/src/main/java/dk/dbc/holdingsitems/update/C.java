/*
 * Copyright (C) 2016 DBC A/S (http://dbc.dk/)
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

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class C {

    public static final String PROPERTIES_LOOKUP = "holdingsitems-update";

    public static final String UPDATE_QUEUE_LIST = "updateQueueList";
    public static final String UPDATE_QUEUE_LIST_DEFAULT = "update-worker";

    public static final String COMPLETE_QUEUE_LIST = "completeQueueList";
    public static final String COMPLETE_QUEUE_LIST_DEFAULT = "complete-worker";

    public static final String ONLINE_QUEUE_LIST = "onlineQueueList";
    public static final String ONLINE_QUEUE_LIST_DEFAULT = "online-worker";

    public static final String DISABLE_AUTHENTICATION = "disableAuthentication";
    public static final String DISABLE_AUTHENTICATION_DEFAULT = "true";

    public static final String FORS_RIGHTS_URL = "forsRightsUrl";

    public static final String MAX_AGE_MINUTES = "maxAgeMinutes";
    public static final String MAX_AGE_MINUTES_DEFAULT = "480"; // 8 Hours

    public static final String RIGHTS_GROUP = "rightsGroup";
    public static final String RIGHTS_NAME = "rightsName";

    public static final String X_FORWARDED_FOR = "xForwardedFor";
    public static final String X_FORWARDED_FOR_DEFAULT = "";

    public static final String DATASOURCE = "jdbc/holdingsitemsupdate/holdingsitems";

}
