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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.idp.marshallers.request.AuthenticationRequest;
import dk.dbc.idp.marshallers.response.AuthenticationResponse;
import dk.dbc.oss.ns.holdingsitemsupdate.Authentication;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
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
    Config config;

    Client client;

    private URI uri;
    private String productName;
    private String name;

    @PostConstruct
    public void setup() {
        uri = config.getIdpUrl().path("authorize").build();
        productName = config.getIdpProductName();
        name = config.getIdpName();
        client = ClientBuilder.newClient();
    }

    /**
     * Check a user against access system
     *
     * @param auth Authentication class from soap request
     * @return The group user is validated to be or null if unvalidated
     */
    public String validate(Authentication auth) {
        AuthenticationRequest req = new AuthenticationRequest();
        req.setUserIdAut(auth.getUserIdAut());
        req.setAgencyId(auth.getGroupIdAut());
        req.setPasswordAut(auth.getPasswordAut());
        log.debug("req = {}", req);
        try {
            String payload = O.writeValueAsString(req);
            try( InputStream is = client.target(uri)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .buildPost(Entity.json(payload))
                    .invoke(InputStream.class)) {

                AuthenticationResponse response = O.readValue(is, AuthenticationResponse.class);
                log.debug("response = {}", response);
                if(response.isAuthenticated() &&
                   response.getRights() != null &&
                   response.getRights().stream()
                           .anyMatch(r -> r.getProductName().equalsIgnoreCase(productName) &&
                                            r.getName().equalsIgnoreCase(name)))
                    return auth.getGroupIdAut();
            } catch(IOException | WebApplicationException ex) {
                log.error("Error fetching data from IDP: {}", ex.getMessage());
                log.debug("Error fetching data from IDP: ", ex);
                throw new FailedUpdateInternalException("Error communicating with IDP service");
            }
            return null;
        } catch(JsonProcessingException ex) {
            log.error("Error parsing json: {}", ex.getMessage());
            log.debug("Error parsing json: ", ex);
            throw new FailedUpdateInternalException("Error communicating with IDP service");
        }
    }
}
