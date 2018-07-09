/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of dbc-holdings-items-access
 *
 * dbc-holdings-items-access is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dbc-holdings-items-access is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class QueueJobTest {


    /**
     * Test of getWorker method, of class QueueJob.
     */
    @Test
    public void testGetWorker() {
        System.out.println("getWorker");
        QueueJob originalJob = new QueueJob(123456, "abc", "{}", "t1");
        QueueJob skippedJob = new QueueJob(123456, "abc", "{}", "t2");

        QueueJob mergedJob = QueueJob.DEDUPLICATION_ABSTRACTION.mergeJob(originalJob, skippedJob);

        System.out.println("mergedJob = " + mergedJob);
        assertEquals("t1\tt2", mergedJob.getTrackingId());
    }
}
