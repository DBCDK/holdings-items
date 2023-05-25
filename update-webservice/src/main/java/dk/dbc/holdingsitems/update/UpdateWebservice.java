/*
 * Copyright (C) 2017-2018 DBC A/S (http://dbc.dk/)
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

import com.sun.xml.ws.developer.SchemaValidation;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteHoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateResult;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateStatusEnum;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineHoldingsItemsUpdateRequest;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;
import org.eclipse.microprofile.metrics.annotation.Timed;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@WebService(serviceName = "HoldingsItemsUpdateServices",
            portName = "HoldingsItemsUpdatePort",
            endpointInterface = "dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdatePortType",
            targetNamespace = "http://oss.dbc.dk/ns/holdingsItemsUpdate",
            wsdlLocation = "WEB-INF/wsdl/holdingsItemsUpdate.wsdl")
@SchemaValidation(handler = WsdlValidationHandler.class, inbound = true, outbound = false)
//@Stateless
public class UpdateWebservice {

    @Inject
    UpdateBean updateBean;

    @Inject StatusCounterBean statusCounterBean;

    @Resource
    WebServiceContext wsc;

    /**
     * Accept request for multiple bibliographic record ids
     * <p>
     * This is for small updates
     *
     * @param req soap request
     * @return soap response
     */
    @Timed
    public HoldingsItemsUpdateResult holdingsItemsUpdate(final HoldingsItemsUpdateRequest req) {
        updateBean.setWebServiceContext(wsc);
        try (StatusCounterBean.Context context = statusCounterBean.count("update")) {
            HoldingsItemsUpdateResult result = updateBean.holdingsItemsUpdate(req);
            if (result.getHoldingsItemsUpdateStatus() == HoldingsItemsUpdateStatusEnum.OK)
                context.success();
            return result;
        }
    }

    /**
     * Request for resetting one collection
     *
     * @param req soap request
     * @return soap response
     */
    @Timed
    public HoldingsItemsUpdateResult completeHoldingsItemsUpdate(final CompleteHoldingsItemsUpdateRequest req) {
        updateBean.setWebServiceContext(wsc);
        try (StatusCounterBean.Context context = statusCounterBean.count("complete")) {
            HoldingsItemsUpdateResult result = updateBean.completeHoldingsItemsUpdate(req);
            if (result.getHoldingsItemsUpdateStatus() == HoldingsItemsUpdateStatusEnum.OK)
                context.success();
            return result;
        }
    }

    /**
     * Request for handling holdings on online resources
     * <p>
     * The issueId for en online holding is an empty string, so is the itemId
     * also. This itemId is not valid for anything else
     *
     * @param req soap request
     * @return soap response
     */
    @Timed
    public HoldingsItemsUpdateResult onlineHoldingsItemsUpdate(final OnlineHoldingsItemsUpdateRequest req) {
        updateBean.setWebServiceContext(wsc);
        try (StatusCounterBean.Context context = statusCounterBean.count("online")) {
            HoldingsItemsUpdateResult result = updateBean.onlineHoldingsItemsUpdate(req);
            if (result.getHoldingsItemsUpdateStatus() == HoldingsItemsUpdateStatusEnum.OK)
                context.success();
            return result;
        }
    }
}
