/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of dbc-git:holdings-items-access
 *
 * dbc-git:holdings-items-access is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dbc-git:holdings-items-access is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems;

import dk.dbc.pgqueue.PreparedQueueSupplier;
import dk.dbc.pgqueue.QueueSupplier;
import dk.dbc.pgqueue.consumer.JobConsumer;
import dk.dbc.pgqueue.consumer.JobMetaData;
import dk.dbc.pgqueue.consumer.QueueWorker;
import java.sql.Connection;
import java.time.Instant;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class QueueJobIT extends DbBase {

    private static final Logger log = LoggerFactory.getLogger(QueueJobIT.class);

    private static final QueueSupplier QUEUE_SUPPLIER = new QueueSupplier(QueueJob.STORAGE_ABSTRACTION);
    private static final String QUEUE = "test-queue";

    @Test(timeout = 5000L)
    public void testStoreRetrieve() throws Exception {
        System.out.println("store-retrieve");

        QueueJob job1 = new QueueJob(888888, "12345678", "t1");
        QueueJob job2 = new QueueJob(888888, "87654321", "t2");

        try (Connection connection = dataSource.getConnection()) {
            PreparedQueueSupplier<QueueJob> supplier = QUEUE_SUPPLIER.preparedSupplier(connection);

            supplier.enqueue(QUEUE, job1);
            supplier.enqueue(QUEUE, job2);

            BlockingDeque<QueueJob> list = new LinkedBlockingDeque<>();

            QueueWorker worker = QueueWorker.builder()
                    .consume(QUEUE)
                    .dataSource(dataSource)
                    .build(QueueJob.STORAGE_ABSTRACTION, (JobConsumer<QueueJob>) (Connection connection1, QueueJob job, JobMetaData metaData) -> {
                       list.add(job);
                   });
            worker.start();

            QueueJob actual1 = list.pollFirst(5, TimeUnit.SECONDS);
            QueueJob actual2 = list.pollFirst(5, TimeUnit.SECONDS);

            worker.stop();

            assertEquals(job1.toString(), actual1.toString());
            assertEquals(job2.toString(), actual2.toString());
        }
    }
}
