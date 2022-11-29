package dk.dbc.holdingsitems.jpa;

import javax.persistence.NamedQuery;
import java.io.Serializable;
import java.util.Map;

// not 100% sure on the query syntax...
@NamedQuery(name = StatusCountEntity.BY_AGENCY, query = "SELECT i.status, COUNT(i) FROM ItemEntity i WHERE i.agencyId = :agencyId GROUP BY i.status")
public class StatusCountEntity implements Serializable {

    // same as in all other entity classes
    private static final long serialVersionUID = 1089023457634768914L;

    // is this a construction to avoid injection?
    public static final String BY_AGENCY = "ItemsEntity.byAgencyId";
    // what I expect to be converting into a StatusCountResponse object ...
    private Map<String, Integer> statusCount;

    // wondering if the query should be in the HoldingsItemsDAO ... ?

}
