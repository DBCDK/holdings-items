/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items-access
 *
 * holdings-items-access is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items-access is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.jpa;

import dk.dbc.holdingsitems.JpaBase;
import java.time.Instant;
import java.util.List;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class JpaIT extends JpaBase {

    @Test(timeout = 2_000L)
    public void testPersistVsMerge() throws Exception {
        System.out.println("testPersistVsMerge");
        jpa(em -> {
            // Newly created: fill is nessecary, save is persist
            HoldingsItemsCollectionEntity c1 = HoldingsItemsCollectionEntity.from(em, 870970, "25912233", "i1", Instant.MIN);
            assertThat(c1.getTrackingId(), nullValue());
            fill(c1);
            c1.setTrackingId("ABC#1");
            c1.save();
        });

        HoldingsItemsCollectionEntity v1 = jpa(em -> {
            return HoldingsItemsCollectionEntity.from(em, 870970, "25912233", "i1", Instant.MIN);
        });
        assertThat(v1.getTrackingId(), is("ABC#1"));

        jpa(em -> {
            // Taken from database: fill is not nessecary, save is merge
            HoldingsItemsCollectionEntity c2 = HoldingsItemsCollectionEntity.from(em, 870970, "25912233", "i1", Instant.MIN);
            c2.setTrackingId("ABC#2");
            c2.save();
        });

        HoldingsItemsCollectionEntity v2 = jpa(em -> {
            return HoldingsItemsCollectionEntity.from(em, 870970, "25912233", "i1", Instant.MIN);
        });
        assertThat(v2.getTrackingId(), is("ABC#2"));
    }

    @Test(timeout = 2_000L)
    public void getAllIssues() throws Exception {
        System.out.println("getAllIssues");

        HoldingsItemsCollectionEntity[] expected = new HoldingsItemsCollectionEntity[2];

        jpa(em -> {
            HoldingsItemsCollectionEntity c1 = HoldingsItemsCollectionEntity.from(em, 870970, "25912233", "i1", Instant.MIN);
            fill(c1);
            c1.save();
            expected[0] = c1;

            HoldingsItemsCollectionEntity c2 = HoldingsItemsCollectionEntity.from(em, 870970, "25912233", "i2", Instant.MIN);
            fill(c2);
            c2.save();
            expected[1] = c2;

            HoldingsItemsCollectionEntity c3 = HoldingsItemsCollectionEntity.from(em, 123456, "25912233", "i2", Instant.MIN);
            fill(c3);
            c3.save();

            HoldingsItemsCollectionEntity c4 = HoldingsItemsCollectionEntity.from(em, 870970, "abc", "i2", Instant.MIN);
            fill(c4);
            c4.save();
        });

        flushAndEvict();
        List<HoldingsItemsCollectionEntity> all = jpa(em -> {
            return HoldingsItemsCollectionEntity.byAgencyBibliographic(em, 870970, "25912233");
        });
        assertThat(all, hasItems(expected));
    }

}
