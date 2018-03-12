/*
 * dbc-holdings-items-update-webservice
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043*
 *
 * This file is part of dbc-holdings-items-update-webservice.
 *
 * dbc-holdings-items-update-webservice is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * dbc-holdings-items-update-webservice is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with dbc-holdings-items-update-webservice.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.update;

import dk.dbc.eeconfig.EEConfig;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Singleton
public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    @PostConstruct
    public void init() {
        splitQueue(updateQueueListReal, s -> updateQueueList = s, s -> updateQueueListOld = s);
        splitQueue(completeQueueListReal, s -> completeQueueList = s, s -> completeQueueListOld = s);
        splitQueue(onlineQueueListReal, s -> onlineQueueList = s, s -> onlineQueueListOld = s);

    }

    private static void splitQueue(String real, Consumer<String> queueByNew, Consumer<String> queueByOld) {
        HashSet<String> byNew = new HashSet<>();
        HashSet<String> byOld = new HashSet<>();
        for (String queue : real.split(",")) {
            if (queue.endsWith(":old")) {
                byOld.add(queue.substring(0, queue.lastIndexOf(':')));
            } else {
                byNew.add(queue);
            }
        }
        queueByNew.accept(String.join(",", byNew));
        queueByOld.accept(String.join(",", byOld));
    }

    @Inject
    @EEConfig.Name(C.UPDATE_QUEUE_LIST)
    @EEConfig.Default(C.UPDATE_QUEUE_LIST_DEFAULT)
    @NotNull
    String updateQueueListReal;

    String updateQueueListOld;

    public String getUpdateQueueList() {
        return updateQueueList;
    }

    String updateQueueList;

    public String getUpdateQueueOldList() {
        return updateQueueListOld;
    }

    @Inject
    @EEConfig.Name(C.COMPLETE_QUEUE_LIST)
    @EEConfig.Default(C.COMPLETE_QUEUE_LIST_DEFAULT)
    @NotNull
    String completeQueueListReal;

    String completeQueueListOld;

    public String getCompleteQueueList() {
        return completeQueueList;
    }
    String completeQueueList;

    public String getCompleteQueueOldList() {
        return completeQueueListOld;
    }

    @Inject
    @EEConfig.Name(C.ONLINE_QUEUE_LIST)
    @EEConfig.Default(C.ONLINE_QUEUE_LIST_DEFAULT)
    @NotNull
    String onlineQueueListReal;

    String onlineQueueListOld;

    public String getOnlineQueueList() {
        return onlineQueueList;
    }

    String onlineQueueList;

    public String getOnlineQueueOldList() {
        return onlineQueueListOld;
    }

    @Inject
    @EEConfig.Name(C.DISABLE_AUTHENTICATION)
    @EEConfig.Default(C.DISABLE_AUTHENTICATION_DEFAULT)
    Boolean disableAuthentication;

    public boolean getDisableAuthentication() {
        return disableAuthentication;
    }

    @Inject
    @EEConfig.Name(C.FORS_RIGHTS_URL)
    @Resource(name = "forsRightsUrl")
    String forsRightsUrl;

    public String getForsRightsUrl() {
        return forsRightsUrl;
    }

    @Inject
    @EEConfig.Name(C.MAX_AGE_MINUTES)
    @EEConfig.Default(C.MAX_AGE_MINUTES_DEFAULT)
    @Min(1)
    Integer maxAgeMinutes;

    public Integer getMaxAgeMinutes() {
        return maxAgeMinutes;
    }

    @Inject
    @EEConfig.Name(C.RIGHTS_GROUP)
    String rightsGroup;

    public String getRightsGroup() {
        return rightsGroup;
    }

    @Inject
    @EEConfig.Name(C.RIGHTS_NAME)
    String rightsName;

    public String getRightsName() {
        return rightsName;
    }

    @Inject
    @EEConfig.Name(C.X_FORWARDED_FOR)
    @EEConfig.Default(C.X_FORWARDED_FOR_DEFAULT)
    String xForwardedFor;
    HashSet<String> xForwardedForSet = null;

    synchronized public Set<String> getXForwardedFor() {
        if (xForwardedForSet == null) {
            xForwardedForSet = new HashSet<>();
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                xForwardedForSet.addAll(Arrays.asList(xForwardedFor.split(";")));
            }
        }
        return xForwardedForSet;
    }

}
