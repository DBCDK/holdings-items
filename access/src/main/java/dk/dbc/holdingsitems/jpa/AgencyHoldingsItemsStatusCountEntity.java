package dk.dbc.holdingsitems.jpa;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.NamedQuery;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** Class to represent the resulting table when asking for an agencys total count holdings-items grouped by status
 * author: sahu@dbc.dk
 */

// not 100% sure this class is needed or if it isn't just unnecessary overhead ...

public class AgencyHoldingsItemsStatusCountEntity implements Serializable {

    // same as in all other entity classes
    private static final long serialVersionUID = 1089023457634768914L;

    private Map<String, Integer> statusCount;

}
