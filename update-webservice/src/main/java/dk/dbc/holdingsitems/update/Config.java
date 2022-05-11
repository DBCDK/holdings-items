/*
 * Copyright (C) 2015-2018 DBC A/S (http://dbc.dk/)
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

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings
 *
 * @author DBC {@literal <dbc.dk>}
 */
@ApplicationScoped
@Singleton
@Startup
@Lock(LockType.READ)
public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    private String updateQueueList;
    private String completeQueueList;
    private String onlineQueueList;
    private Set<Integer> shouldLogXmlAgenciesSet;
    private boolean disableAuthentication;
    private UriBuilder idpUrl;
    private String idpProductName;
    private String idpName;

    private final Map<String, String> env;

    public Config() {
        this.env = System.getenv();
    }

    public Config(String... strs) {
        env = Arrays.stream(strs)
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));
        configure();
    }

    @PostConstruct
    public void init() {
        configure();
    }

    private void configure() {
        updateQueueList = get("UPDATE_QUEUE_LIST");
        completeQueueList = get("COMPLETE_QUEUE_LIST");
        onlineQueueList = get("ONLINE_QUEUE_LIST");
        shouldLogXmlAgenciesSet = Arrays.stream(get("DEBUG_XML_AGENCIES", "").split(";"))
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
        log.debug("shouldLogXmlAgenciesSet = {}", shouldLogXmlAgenciesSet);
        disableAuthentication = Boolean.valueOf(get("DISABLE_AUTHENTICATION", "false"));
        idpUrl = UriBuilder.fromUri(get("IDP_URL"));
        String[] idpRule = get("IDP_RIGHTS").split(",");
        if (idpRule.length != 2)
            throw new EJBException("Invalid required configuration: IDP_RIGHTS, doesn't have format (product,name)");
        idpProductName = idpRule[0];
        idpName = idpRule[1];

    }

    private String get(String var) {
        String value = env.get(var);
        if (value == null)
            throw new EJBException("Missing required configuration: " + var);
        return value;
    }

    private String get(String var, String defaultValue) {
        return env.getOrDefault(var, defaultValue);
    }

    public String getUpdateQueueList() {
        return updateQueueList;
    }

    public String getCompleteQueueList() {
        return completeQueueList;
    }

    public String getOnlineQueueList() {
        return onlineQueueList;
    }

    public boolean getDisableAuthentication() {
        return disableAuthentication;
    }

    public UriBuilder getIdpUrl() {
        return idpUrl.clone();
    }

    public String getIdpProductName() {
        return idpProductName;
    }

    public String getIdpName() {
        return idpName;
    }

    public boolean shouldLogXml(int agencyId) {
        return shouldLogXmlAgenciesSet.contains(agencyId);
    }
}
