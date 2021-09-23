package dk.dbc.holdingsitems.update;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import org.eclipse.microprofile.metrics.annotation.Counted;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
// @Singleton @Startup to ensure the counters are ready as soon as the war is deployed
@Singleton
@Startup
@Lock(LockType.READ)
public class StatusCounterBean {

    @Counted(name = "request-success", absolute = true, tags = {"request=holdings-items-update"})
    public void success() {
    }

    @Counted(name = "request-failure", absolute = true, tags = {"request=holdings-items-update"})
    public void failure() {
    }

}
