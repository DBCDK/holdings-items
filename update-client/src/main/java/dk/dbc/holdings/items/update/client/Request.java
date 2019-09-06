/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
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
package dk.dbc.holdings.items.update.client;

import dk.dbc.oss.ns.holdingsitemsupdate.BibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteBibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItem;
import dk.dbc.oss.ns.holdingsitemsupdate.ModificationTimeStamp;
import dk.dbc.oss.ns.holdingsitemsupdate.StatusType;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class Request {

    /**
     * Converts holdingsXML to holdings items request.
     *
     * @param xml        holdings xml
     * @param trackingId id for tracking
     * @return List of requests
     */
    public static List<CompleteUpdateRequest> fromHoldingsXML(String xml, String trackingId) {

        try {
            List<Holding> holdings = unmarshallHoldingsXML(xml);
            List<CompleteUpdateRequest> requests = new ArrayList<>();

            for (Holding holding : holdings) {
                requests.add(fromHolding(holding, trackingId));
            }

            return requests;
        } catch (JAXBException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Converts single holding to a CompleteUpdateRequest.
     * <p>
     * See http://wiki.dbc.dk/bin/view/Data/PhHoldingsItemTilHoldingsXml
     *
     * @param holding    holdings data structure
     * @param trackingId id for tracking
     * @return a single request
     */
    static CompleteUpdateRequest fromHolding(Holding holding, String trackingId) {

        try {
            GregorianCalendar c = new GregorianCalendar();
            c.setTime(new Date());
            XMLGregorianCalendar xmlCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);

            ModificationTimeStamp modificationTimeStamp = new ModificationTimeStamp();
            modificationTimeStamp.setModificationDateTime(xmlCalendar);

            CompleteBibliographicItem item = new CompleteBibliographicItem();
            item.setBibliographicRecordId(holding.getIdentifier());
            item.setModificationTimeStamp(modificationTimeStamp);
            item.setNote("");

            dk.dbc.oss.ns.holdingsitemsupdate.Holding h = new dk.dbc.oss.ns.holdingsitemsupdate.Holding();
            h.setIssueId("");
            h.setIssueText("");

            HoldingsItem holdingsItem = new HoldingsItem();

            holdingsItem.setAccessionDate(xmlCalendar);
            holdingsItem.setBranch("");
            holdingsItem.setCirculationRule("");
            holdingsItem.setDepartment("");
            holdingsItem.setItemId(holding.identifier);
            holdingsItem.setLocation("");
            holdingsItem.setSubLocation("");

            StatusType statusType = getStatusType(holding.getState(), holding.getCode());
            holdingsItem.setStatus(statusType);

            if (statusType == StatusType.ON_LOAN || statusType == StatusType.ON_SHELF) {
                h.setReadyForLoan(BigInteger.valueOf(Math.max(1, holding.getCount())));
            } else {
                h.setReadyForLoan(BigInteger.ZERO);
            }

            item.getHoldings().add(h);
            h.getHoldingsItems().add(holdingsItem);

            return new CompleteUpdateRequest(holding.getMaterialOwner(), item, trackingId);
        } catch (DatatypeConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Maps state and code of HoldingsXML to a holdings item status.
     * <p>
     * See http://wiki.dbc.dk/bin/view/Data/PhHoldingsItemTilHoldingsXml
     *
     * @param state deleted or alive
     * @param code  lending rule
     * @return status element
     */
    static StatusType getStatusType(String state, String code) {
        if (state.equals("d")) {
            return StatusType.DECOMMISSIONED;
        } else {
            if (code == null) {
                // Could be either ON_LOAN or ON_SHELF
                return StatusType.ON_LOAN;
            } else {
                switch (code) {
                    case "a":
                        // Could be either NOT_FOR_LOAN or ONLINE
                        return StatusType.NOT_FOR_LOAN;
                    case "e":
                        return StatusType.ON_ORDER;
                    default:
                        return StatusType.NOT_FOR_LOAN;
                }
            }
        }
    }

    static List<Holding> unmarshallHoldingsXML(String xml) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Holdings.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        try (StringReader reader = new StringReader(xml)) {
            Holdings res = (Holdings) jaxbUnmarshaller.unmarshal(reader);
            return res.getHoldings();
        }
    }

    @XmlRootElement(name = "holdings")
    public static class Holdings {

        private List<Holding> holdings;

        public List<Holding> getHoldings() {
            return holdings;
        }

        @XmlElement(name = "holding")
        public void setHoldings(List<Holding> holdings) {
            this.holdings = holdings;
        }
    }

    public static class Holding {

        private String type;
        private String pid;
        private String dataOwner;
        private String format;
        private String identifier;
        private String materialOwner;
        private String state;

        private String orderId;
        private String callNumber;
        private String code;
        private String note;
        private int count;

        public String getType() {
            return type;
        }

        @XmlElement
        public void setType(String type) {
            this.type = type;
        }

        public String getPid() {
            return pid;
        }

        @XmlElement
        public void setPid(String pid) {
            this.pid = pid;
        }

        public String getDataOwner() {
            return dataOwner;
        }

        @XmlElement
        public void setDataOwner(String dataOwner) {
            this.dataOwner = dataOwner;
        }

        public String getFormat() {
            return format;
        }

        @XmlElement
        public void setFormat(String format) {
            this.format = format;
        }

        public String getIdentifier() {
            return identifier;
        }

        @XmlElement
        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public String getMaterialOwner() {
            return materialOwner;
        }

        @XmlElement
        public void setMaterialOwner(String materialOwner) {
            this.materialOwner = materialOwner;
        }

        public String getState() {
            return state;
        }

        @XmlElement
        public void setState(String state) {
            this.state = state;
        }

        public String getOrderId() {
            return orderId;
        }

        @XmlElement
        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getCallNumber() {
            return callNumber;
        }

        @XmlElement
        public void setCallNumber(String callNumber) {
            this.callNumber = callNumber;
        }

        public String getCode() {
            return code;
        }

        @XmlElement
        public void setCode(String code) {
            this.code = code;
        }

        public String getNote() {
            return note;
        }

        @XmlElement
        public void setNote(String note) {
            this.note = note;
        }

        public int getCount() {
            return count;
        }

        @XmlElement
        public void setCount(int count) {
            this.count = count;
        }
    }

    public static class UpdateRequest {

        private final String agencyId;
        private final List<BibliographicItem> items;
        private final String trackingId;

        public UpdateRequest(String agencyId, List<BibliographicItem> items, String trackingId) {
            this.agencyId = agencyId;
            this.items = items;
            this.trackingId = trackingId;
        }

        public String getAgencyId() {
            return agencyId;
        }

        public List<BibliographicItem> getItems() {
            return items;
        }

        public String getTrackingId() {
            return trackingId;
        }

    }

    public static class CompleteUpdateRequest {

        private final String agencyId;
        private final CompleteBibliographicItem item;
        private final String trackingId;

        public CompleteUpdateRequest(String agencyId, CompleteBibliographicItem item, String trackingId) {
            this.agencyId = agencyId;
            this.item = item;
            this.trackingId = trackingId;
        }

        public String getAgencyId() {
            return agencyId;
        }

        public CompleteBibliographicItem getItem() {
            return item;
        }

        public String getTrackingId() {
            return trackingId;
        }
    }
}
