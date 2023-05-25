/*
 * Copyright (C) 2021 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items-purge-tool
 *
 * holdings-items-purge-tool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items-purge-tool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.purge;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.vipcore.marshallers.FindLibraryResponse;
import dk.dbc.vipcore.marshallers.PickupAgency;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.JerseyClientBuilder;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class VipCoreConnector {

    private static final String UNKNOWN = "<unknown>";
    private static final ObjectMapper O = new ObjectMapper()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final Client client;
    private final UriBuilder baseUri;

    public VipCoreConnector(String url) {
        this.client = JerseyClientBuilder.newBuilder()
                .build();
        this.baseUri = UriBuilder.fromUri(URI.create(url))
                .path("findlibrary");
    }

    public String lookupAgencyName(int id) throws IOException {
        return lookupAgencyName(String.valueOf(id));
    }

    public String lookupAgencyName(String id) throws IOException {
        URI uri = this.baseUri.clone()
                .path(id)
                .build();
        try (InputStream is = client.target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(InputStream.class)) {
            FindLibraryResponse val = O.readValue(is, FindLibraryResponse.class);
            Iterator<PickupAgency> iter = val.getPickupAgency().iterator();
            if (iter.hasNext()) {
                PickupAgency agency = iter.next();
                String agencyName = agency.getAgencyName();
                if (agencyName != null)
                    return agencyName;
            }
            return UNKNOWN;
        } catch (ClientErrorException ex) {
            return UNKNOWN;
        }
    }

}
