package dk.dbc.holdingsitems.update;

import javax.ejb.Stateless;
import org.eclipse.microprofile.metrics.annotation.Counted;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
public class StatusCounterBean {

    @Counted(name = "request-success")
    public void success() {
    }

    @Counted(name = "request-failure")
    public void failure() {
    }

}
