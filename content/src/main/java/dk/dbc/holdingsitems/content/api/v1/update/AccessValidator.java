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
package dk.dbc.holdingsitems.content.api.v1.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.idp.marshallers.request.AuthenticationRequest;
import dk.dbc.idp.marshallers.response.AuthenticationResponse;
import dk.dbc.oss.ns.holdingsitemsupdate.Authentication;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateStatusEnum;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Stateless
public class AccessValidator {

    private static final Logger log = LoggerFactory.getLogger(AccessValidator.class);

    private static final ObjectMapper O = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Inject
    @ConfigProperty(name = "DISABLE_AUTHENTICATION", defaultValue = "false")
    boolean disableAuthentication;

    @Inject
    @ConfigProperty(name = "IDP_URL")
    String idpUrl;

    @Inject
    @ConfigProperty(name = "IDP_RIGHTS")
    String idpRights;

    private Client client;

    private URI uri;
    private String productName;
    private String name;

    @PostConstruct
    public void setup() {
        client = ClientBuilder.newClient();

        uri = UriBuilder.fromUri(idpUrl).path("authorize").build();

        String[] idpRule = idpRights.split(",");
        if (idpRule.length != 2)
            throw new EJBException("Invalid required configuration: IDP_RIGHTS, doesn't have format (product,name)");
        productName = idpRule[0];
        name = idpRule[1];
    }

    /**
     * Check a user against access system
     *
     * @param auth             Authentication class from soap request
     * @param expectedAgencyId what agency user is expected to be from
     * @throws UpdateException In case of validation error
     */
    public void validate(Authentication auth, int expectedAgencyId) throws UpdateException {
        if (auth == null) {
            if (disableAuthentication) {
                return;
            }
            throw new UpdateException(HoldingsItemsUpdateStatusEnum.AUTHENTICATION_ERROR, "Missing required authentication");
        }

        AuthenticationRequest req = new AuthenticationRequest();
        req.withUserIdAut(auth.getUserIdAut());
        req.withAgencyId(auth.getGroupIdAut());
        req.withPasswordAut(auth.getPasswordAut());
        log.debug("req = {}", req);
        try {
            String payload = O.writeValueAsString(req);
            try (InputStream is = client.target(uri)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .buildPost(Entity.json(payload))
                    .invoke(InputStream.class)) {
                AuthenticationResponse response = O.readValue(is, AuthenticationResponse.class);
                log.debug("response = {}", response);
                if (response.isAuthenticated() &&
                    response.getRights() != null &&
                    response.getRights().stream()
                            .anyMatch(r -> r.getProductName().equalsIgnoreCase(productName) &&
                                             r.getName().equalsIgnoreCase(name))) {
                    int actualAgencyId = Integer.valueOf(auth.getGroupIdAut());
                    if (actualAgencyId != expectedAgencyId)
                        throw new UpdateException(HoldingsItemsUpdateStatusEnum.FAILED_INVALID_AGENCY, "Authenticated agencyId mismatched updated agencyId");
                } else {
                    throw new UpdateException(HoldingsItemsUpdateStatusEnum.AUTHENTICATION_ERROR, "Invalid authentication - update not allowed");
                }
            } catch (IOException | WebApplicationException ex) {
                log.error("Error fetching data from IDP: {}", ex.getMessage());
                log.debug("Error fetching data from IDP: ", ex);
                throw new UpdateException(HoldingsItemsUpdateStatusEnum.AUTHENTICATION_ERROR, "Invalid authentication");
            }
        } catch (JsonProcessingException ex) {
            log.error("Error parsing json: {}", ex.getMessage());
            log.debug("Error parsing json: ", ex);
            throw new UpdateException(HoldingsItemsUpdateStatusEnum.FAILED_UPDATE_INTERNAL_ERROR, "Cannot process authorization response");
        }
    }
}
