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

import dk.dbc.forsrights.client.ForsRights;
import dk.dbc.forsrights.client.ForsRightsException;
import dk.dbc.forsrights.client.ForsRightsServiceFromURL;
import dk.dbc.oss.ns.holdingsitemsupdate.Authentication;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Stateless
public class AccessValidator {

    private static final Logger log = LoggerFactory.getLogger(AccessValidator.class);

    @Inject
    Config config;

    @Inject
    AccessCache accessCache;

    ForsRightsServiceFromURL service;
    ForsRights forsRights;

    @PostConstruct
    public void setup() {
        service = ForsRightsServiceFromURL.builder().build(config.getForsRightsUrl());
        forsRights = service.forsRights(accessCache.cache);
    }

    /**
     * Check a user against access system
     *
     * @param auth        Authentication class from soap request
     * @param ip          ip number from remote host
     * @param rightsGroup forsrights access group
     * @param rightsName  forsrights access name
     * @return if the user has been validated
     * @throws ForsRightsException in case of communication errors with
     *                             forsrights.
     */
    public boolean validate(Authentication auth, String ip, String rightsGroup, String rightsName) throws ForsRightsException {
        try {
            if (auth != null) {
                String user = auth.getUserIdAut();
                String group = auth.getGroupIdAut();
                String password = auth.getPasswordAut();
                log.trace("validate user: {}, group: {}", user, group);
                return forsRights.lookupRight(user, group, password, null)
                        .hasRight(rightsGroup, rightsName);
            } else {
                log.trace("validate ip: {}", ip);
                return forsRights.lookupRight(null, null, null, ip)
                        .hasRight(rightsGroup, rightsName);
            }
        } catch (RuntimeException ex) {
            log.trace("RuntimeException: {}", ex.getMessage());
            throw new ForsRightsException(ex);
        }
    }

}
