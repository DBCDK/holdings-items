package dk.dbc.holdings.items.update.client;

import dk.dbc.oss.ns.holdingsitemsupdate.StatusType;
import java.math.BigInteger;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class RequestTest {

    @Test
    public void testUnmarshallHoldingsXML() throws Exception {
        
        String holdingsXML = "<holdings>" +
                                "<holding>" +
                                    "<type>record</type>" +
                                    "<pid>830370-katalog:01284592</pid>" +
                                    "<dataOwner>830370</dataOwner>" +
                                    "<format>katalog</format>" +
                                    "<identifier>01284592</identifier>" +
                                    "<materialOwner>830370</materialOwner>" +
                                    "<state>a</state>" +
                                "</holding>" +
                                "<holding>" +
                                    "<type>record</type>" +
                                    "<pid>870970-basis:01284592</pid>" +
                                    "<dataOwner>870970</dataOwner>" +
                                    "<format>basis</format>" +
                                    "<identifier>01284592</identifier>" +
                                    "<materialOwner>870970</materialOwner>" +
                                    "<count>7</count>" +
                                    "<state>a</state>" +
                                "</holding>" +
                             "</holdings>";
        List<Request.Holding> holdings = Request.unmarshallHoldingsXML(holdingsXML);        
        Request.Holding h1 = holdings.get(0);
        Request.Holding h2 = holdings.get(1);

        assertEquals("record", h1.getType());
        assertEquals("830370-katalog:01284592", h1.getPid());
        assertEquals("830370", h1.getDataOwner());
        assertEquals("katalog", h1.getFormat());
        assertEquals("01284592", h1.getIdentifier());
        assertEquals("830370", h1.getMaterialOwner());
        assertEquals("a", h1.getState());
        
        assertEquals("record", h2.getType());
        assertEquals("870970-basis:01284592", h2.getPid());
        assertEquals("870970", h2.getDataOwner());
        assertEquals("basis", h2.getFormat());
        assertEquals("01284592", h2.getIdentifier());
        assertEquals("870970", h2.getMaterialOwner());
        assertEquals(7, h2.getCount());
        assertEquals("a", h2.getState());
    }
    
    @Test
    public void testFromHoldingsXML_onLoan() {
        
        String holdingsXML = "<holdings>" +
                                "<holding>" +
                                    "<type>record</type>" +
                                    "<pid>830370-katalog:01284592</pid>" +
                                    "<dataOwner>830370</dataOwner>" +
                                    "<format>katalog</format>" +
                                    "<identifier>01284592</identifier>" +
                                    "<materialOwner>830380</materialOwner>" +
                                    "<state>a</state>" +
                                    "<orderId>01284592</orderId>" +
                                    "<count>7</count>" +
                                "</holding>" +
                             "</holdings>";
        String trackingId = "trackingId";
        
        List<Request.CompleteUpdateRequest> actualRequests = Request.fromHoldingsXML(holdingsXML, trackingId);
        Request.CompleteUpdateRequest actualRequest = actualRequests.get(0);
        assertEquals("830380", actualRequest.getAgencyId());
        assertEquals(trackingId, actualRequest.getTrackingId());
        assertEquals("01284592", actualRequest.getItem().getBibliographicRecordId());
        assertEquals(BigInteger.valueOf(7), actualRequest.getItem().getHolding().get(0).getReadyForLoan());
        assertEquals(StatusType.ON_LOAN, actualRequest.getItem().getHolding().get(0).getHoldingsItem().get(0).getStatus());
        assertEquals("01284592", actualRequest.getItem().getHolding().get(0).getHoldingsItem().get(0).getItemId());
    }
    
    @Test
    public void testFromHoldingsXML_deletedHolding() {
        
        String holdingsXML = "<holdings>" +
                                "<holding>" +
                                    "<type>record</type>" +
                                    "<pid>830370-katalog:01284592</pid>" +
                                    "<dataOwner>830370</dataOwner>" +
                                    "<format>katalog</format>" +
                                    "<identifier>01284592</identifier>" +
                                    "<materialOwner>830380</materialOwner>" +
                                    "<state>d</state>" +                                    
                                "</holding>" +
                             "</holdings>";
        String trackingId = "trackingId";
        
        List<Request.CompleteUpdateRequest> actualRequests = Request.fromHoldingsXML(holdingsXML, trackingId);
        Request.CompleteUpdateRequest actualRequest = actualRequests.get(0);
        assertEquals("830380", actualRequest.getAgencyId());
        assertEquals(trackingId, actualRequest.getTrackingId());
        assertEquals("01284592", actualRequest.getItem().getBibliographicRecordId());
        assertEquals(BigInteger.ZERO, actualRequest.getItem().getHolding().get(0).getReadyForLoan());
        assertEquals(StatusType.DECOMMISSIONED, actualRequest.getItem().getHolding().get(0).getHoldingsItem().get(0).getStatus());
        assertEquals("01284592", actualRequest.getItem().getHolding().get(0).getHoldingsItem().get(0).getItemId());
    }
    
    @Test
    public void testFromHoldingsXML_multipleHoldings() {
        
        String holdingsXML = "<holdings>" +
                                "<holding>" +
                                    "<type>record</type>" +
                                    "<pid>830370-katalog:01284592</pid>" +
                                    "<dataOwner>830370</dataOwner>" +
                                    "<format>katalog</format>" +
                                    "<identifier>01284592</identifier>" +
                                    "<materialOwner>830370</materialOwner>" +
                                    "<state>d</state>" +
                                "</holding>" +
                                "<holding>" +
                                    "<type>record</type>" +
                                    "<pid>830370-katalog:01284592</pid>" +
                                    "<dataOwner>830370</dataOwner>" +
                                    "<format>katalog</format>" +
                                    "<identifier>01284592</identifier>" +
                                    "<materialOwner>830380</materialOwner>" +
                                    "<state>a</state>" +
                                "</holding>" +
                             "</holdings>";
        String trackingId = "trackingId";
        
        List<Request.CompleteUpdateRequest> actualRequests = Request.fromHoldingsXML(holdingsXML, trackingId);
        Request.CompleteUpdateRequest actualRequest = actualRequests.get(0);
        assertEquals("830370", actualRequest.getAgencyId());
        assertEquals(trackingId, actualRequest.getTrackingId());
        assertEquals("01284592", actualRequest.getItem().getBibliographicRecordId());
        assertEquals(BigInteger.ZERO, actualRequest.getItem().getHolding().get(0).getReadyForLoan());
        assertEquals(StatusType.DECOMMISSIONED, actualRequest.getItem().getHolding().get(0).getHoldingsItem().get(0).getStatus());
        assertEquals("01284592", actualRequest.getItem().getHolding().get(0).getHoldingsItem().get(0).getItemId());
        
        Request.CompleteUpdateRequest actualRequest2 = actualRequests.get(1);
        assertEquals("830380", actualRequest2.getAgencyId());
        assertEquals(trackingId, actualRequest2.getTrackingId());
        assertEquals("01284592", actualRequest2.getItem().getBibliographicRecordId());
        assertEquals(BigInteger.ONE, actualRequest2.getItem().getHolding().get(0).getReadyForLoan());
        assertEquals(StatusType.ON_LOAN, actualRequest2.getItem().getHolding().get(0).getHoldingsItem().get(0).getStatus());
        assertEquals("01284592", actualRequest2.getItem().getHolding().get(0).getHoldingsItem().get(0).getItemId());
    }
    
    @Test
    public void testGetStatusType() {
        assertEquals(StatusType.DECOMMISSIONED, Request.getStatusType("d", null));
        assertEquals(StatusType.ON_LOAN, Request.getStatusType("a", null));
        assertEquals(StatusType.NOT_FOR_LOAN, Request.getStatusType("a", "a"));
        assertEquals(StatusType.NOT_FOR_LOAN, Request.getStatusType("a", "g"));
        assertEquals(StatusType.ON_ORDER, Request.getStatusType("a", "e"));        
    }
    
}
