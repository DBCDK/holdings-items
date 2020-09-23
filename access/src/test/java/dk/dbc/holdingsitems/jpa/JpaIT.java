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
import java.time.LocalDate;
import java.util.List;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

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
            BibliographicItemEntity b1 = BibliographicItemEntity.from(em, 870970, "25912233", Instant.now(), LocalDate.now());
            fill(b1);
            b1.setTrackingId("TOPLEVEL");
            IssueEntity c1 = b1.issue("i1", Instant.MIN);
            assertThat(c1.getTrackingId(), is("TOPLEVEL"));
            fill(c1);
            c1.setTrackingId("ABC#1");
            c1.save();
        });

        IssueEntity v1 = jpa(em -> {
            BibliographicItemEntity b1 = BibliographicItemEntity.from(em, 870970, "25912233", Instant.now(), LocalDate.now());
            return b1.issue("i1", Instant.MIN);
        });
        assertThat(v1.getTrackingId(), is("ABC#1"));

        jpa(em -> {
            // Taken from database: fill is not nessecary, save is merge
            BibliographicItemEntity b2 = BibliographicItemEntity.from(em, 870970, "25912233", Instant.now(), LocalDate.now());
            IssueEntity c2 = b2.issue("i1", Instant.MIN);
            c2.setTrackingId("ABC#2");
            c2.save();
        });

        IssueEntity v2 = jpa(em -> {
            BibliographicItemEntity b2 = BibliographicItemEntity.from(em, 870970, "25912233", Instant.now(), LocalDate.now());
            return b2.issue("i1", Instant.MIN);
        });
        assertThat(v2.getTrackingId(), is("ABC#2"));
    }

    @Test(timeout = 2_000L)
    public void getAllIssues() throws Exception {
        System.out.println("getAllIssues");

        IssueEntity[] expected = new IssueEntity[2];

        jpa(em -> {

            BibliographicItemEntity b1 = BibliographicItemEntity.from(em, 870970, "25912233", Instant.MIN, LocalDate.now());
            fill(b1);

            IssueEntity c1 = b1.issue("i1", Instant.MIN);
            fill(c1);
            ItemEntity i1 = c1.item("a", Instant.MIN);
            fill(i1);

            IssueEntity c2 = b1.issue("i2", Instant.MIN);
            fill(c2);
            ItemEntity i2 = c2.item("b", Instant.MIN);
            fill(i2);
            b1.save();

            BibliographicItemEntity b2 = BibliographicItemEntity.from(em, 123456, "25912233", Instant.MIN, LocalDate.now());
            fill(b2);

            IssueEntity c3 = b2.issue("i1", Instant.MIN);
            fill(c3);
            ItemEntity i3 = c3.item("c", Instant.MIN);
            fill(i3);
            b2.save();

            BibliographicItemEntity b3 = BibliographicItemEntity.from(em, 870970, "abc", Instant.MIN, LocalDate.now());
            fill(b3);

            IssueEntity c4 = b3.issue("i1", Instant.MIN);
            fill(c4);
            ItemEntity i4 = c4.item("d", Instant.MIN);
            fill(i4);
            b3.save();

            expected[0] = c1;
            expected[1] = c2;
        });

        flushAndEvict();
        List<IssueEntity> all = jpa(em -> {
            return IssueEntity.byAgencyBibliographic(em, 870970, "25912233");
        });
        assertThat(all, hasItems(expected));
    }

}
