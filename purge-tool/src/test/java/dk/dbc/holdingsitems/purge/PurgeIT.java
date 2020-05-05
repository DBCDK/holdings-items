/*
 * Copyright (C) 2020 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items-purge-tool
 *
 * holdings-items-purge-tool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items-purge-tool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.purge;

import org.junit.Test;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class PurgeIT extends JpaBase {

    @Test(timeout = 2_000L)
    public void testCase() throws Exception {
        System.out.println("testCase");

        String[] unchanged1 = list("700001/12345678//a1/OnShelf",
                                   "700001/12345678//a2/OnShelf");

        String[] unchanged2 = list("700002/12345678//b1/OnShelf",
                                   "700002/12345678//b2/OnShelf");

        insert(unchanged1);
        insert(unchanged2);

        PurgeReport report = exec(dao -> {
            return new PurgeReport(dao, 700001);
        });

        report.statusReport();

        verify(unchanged1, unchanged2);
    }

}
