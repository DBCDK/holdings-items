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
     * @param xml
     * @param trackingId
     * @return
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
     *
     * See http://wiki.dbc.dk/bin/view/Data/PhHoldingsItemTilHoldingsXml
     *
     * @param holding
     * @param trackingId
     * @return
     */
    static CompleteUpdateRequest fromHolding(Holding holding, String trackingId){

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

            if(statusType == StatusType.ON_LOAN || statusType == StatusType.ON_SHELF) {
                h.setReadyForLoan(BigInteger.valueOf(Math.max(1, holding.getCount())));
            } else {
                h.setReadyForLoan(BigInteger.ZERO);
            }

            item.getHolding().add(h);
            h.getHoldingsItem().add(holdingsItem);

            return new CompleteUpdateRequest(holding.getMaterialOwner(), item, trackingId);
        } catch (DatatypeConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Maps state and code of HoldingsXML to a holdings item status.
     *
     * See http://wiki.dbc.dk/bin/view/Data/PhHoldingsItemTilHoldingsXml
     *
     * @param state
     * @param code
     * @return
     */
    static StatusType getStatusType(String state, String code){
        if(state.equals("d")) {
            return StatusType.DECOMMISSIONED;
        } else {
            if(code == null) {
                // Could be either ON_LOAN or ON_SHELF
                return StatusType.ON_LOAN;
            } else {
                switch(code) {
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
        try(StringReader reader = new StringReader(xml)) {
            Holdings res = (Holdings) jaxbUnmarshaller.unmarshal(reader);
            return res.getHoldings();
        }
    }

    @XmlRootElement(name = "holdings")
    public static class Holdings {

        private List<Holding> holdings;

        /**
         * @return the holdings
         */
        public List<Holding> getHoldings() {
            return holdings;
        }

        /**
         * @param holdings the holdings to set
         */
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




        /**
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * @param type the type to set
         */
        @XmlElement
        public void setType(String type) {
            this.type = type;
        }

        /**
         * @return the pid
         */
        public String getPid() {
            return pid;
        }

        /**
         * @param pid the pid to set
         */
        @XmlElement
        public void setPid(String pid) {
            this.pid = pid;
        }

        /**
         * @return the dataOwner
         */
        public String getDataOwner() {
            return dataOwner;
        }

        /**
         * @param dataOwner the dataOwner to set
         */
        @XmlElement
        public void setDataOwner(String dataOwner) {
            this.dataOwner = dataOwner;
        }

        /**
         * @return the format
         */
        public String getFormat() {
            return format;
        }

        /**
         * @param format the format to set
         */
        @XmlElement
        public void setFormat(String format) {
            this.format = format;
        }

        /**
         * @return the identifier
         */
        public String getIdentifier() {
            return identifier;
        }

        /**
         * @param identifier the identifier to set
         */
        @XmlElement
        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        /**
         * @return the materialOwner
         */
        public String getMaterialOwner() {
            return materialOwner;
        }

        /**
         * @param materialOwner the materialOwner to set
         */
        @XmlElement
        public void setMaterialOwner(String materialOwner) {
            this.materialOwner = materialOwner;
        }

        /**
         * @return the state
         */
        public String getState() {
            return state;
        }

        /**
         * @param state the state to set
         */
        @XmlElement
        public void setState(String state) {
            this.state = state;
        }

        /**
         * @return the orderId
         */
        public String getOrderId() {
            return orderId;
        }

        /**
         * @param orderId the orderId to set
         */
        @XmlElement
        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        /**
         * @return the callNumber
         */
        public String getCallNumber() {
            return callNumber;
        }

        /**
         * @param callNumber the callNumber to set
         */
        @XmlElement
        public void setCallNumber(String callNumber) {
            this.callNumber = callNumber;
        }

        /**
         * @return the code
         */
        public String getCode() {
            return code;
        }

        /**
         * @param code the code to set
         */
        @XmlElement
        public void setCode(String code) {
            this.code = code;
        }

        /**
         * @return the note
         */
        public String getNote() {
            return note;
        }

        /**
         * @param note the note to set
         */
        @XmlElement
        public void setNote(String note) {
            this.note = note;
        }

        /**
         * @return the count
         */
        public int getCount() {
            return count;
        }

        /**
         * @param count the count to set
         */
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

        /**
         * @return the agencyId
         */
        public String getAgencyId() {
            return agencyId;
        }

        /**
         * @return the items
         */
        public List<BibliographicItem> getItems() {
            return items;
        }

        /**
         * @return the trackingId
         */
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

        /**
         * @return the agencyId
         */
        public String getAgencyId() {
            return agencyId;
        }

        /**
         * @return the item
         */
        public CompleteBibliographicItem getItem() {
            return item;
        }

        /**
         * @return the trackingId
         */
        public String getTrackingId() {
            return trackingId;
        }
    }
}


