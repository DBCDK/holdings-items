package dk.dbc.holdingsitems;

import dk.dbc.pgqueue.supplier.PreparedQueueSupplier;
import dk.dbc.pgqueue.supplier.QueueSupplier;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class EnqueueService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(EnqueueService.class);

    private static final QueueSupplier QUEUE_SUPPLIER = new QueueSupplier(QueueJob.STORAGE_ABSTRACTION);

    private final PreparedQueueSupplier<QueueJob> queueSupplier;
    private final String trackingId;
    private final Map<String, List<ConsumerTarget>> consumers;

    EnqueueService(Connection connection, String trackingId) throws HoldingsItemsException {
        this.queueSupplier = QUEUE_SUPPLIER.preparedSupplier(connection);
        this.trackingId = trackingId;
        this.consumers = new HashMap<>();
        try (Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("SELECT supplier, consumer, postpone FROM queue_rules")) {
            while (resultSet.next()) {
                int i = 0;
                String supplier = resultSet.getString(++i);
                String consumer = resultSet.getString(++i);
                Long postpone = resultSet.getLong(++i);
                if (resultSet.wasNull())
                    postpone = null;
                consumers.computeIfAbsent(supplier, s -> new ArrayList<>()).add(new ConsumerTarget(consumer, postpone));
            }
        } catch (SQLException ex) {
            throw new HoldingsItemsException("Cannot create supplier->consumer map", ex);
        }
    }

    /**
     * Put an element into the queue
     *
     * @param supplier              who is producing the job
     * @param bibliographicRecordId part of job id
     * @param agencyId              part of job id
     * @throws HoldingsItemsException in case of a database error
     */
    public void enqueue(String supplier, int agencyId, String bibliographicRecordId) throws HoldingsItemsException {
        enqueue(supplier, agencyId, bibliographicRecordId, "{}");
    }

    /**
     * Put an element into the queue
     *
     * @param supplier              who is producing the job
     * @param bibliographicRecordId part of job id
     * @param agencyId              part of job id
     * @param stateChange           part of a job
     * @throws HoldingsItemsException in case of a database error
     */
    public void enqueue(String supplier, int agencyId, String bibliographicRecordId, String stateChange) throws HoldingsItemsException {
        QueueJob queueJob = new QueueJob(agencyId, bibliographicRecordId, stateChange, trackingId);
        enqueue(supplier, queueJob);
    }

    /**
     * Put an element into the queue
     *
     * @param supplier who is producing the job
     * @param queueJob the job
     * @throws HoldingsItemsException in case of a database error
     */
    public void enqueue(String supplier, QueueJob queueJob) throws HoldingsItemsException {
        try {
            for (ConsumerTarget target : consumers.getOrDefault(supplier, Collections.emptyList())) {
                if (target.postpone == null) {
                    queueSupplier.enqueue(target.consumer, queueJob);
                } else {
                    queueSupplier.enqueue(target.consumer, queueJob, target.postpone);
                }
            }
        } catch (SQLException ex) {
            log.error("Enqueue error", ex);
            throw new HoldingsItemsException("Enqueue error", ex);
        }
    }

    @Override
    public void close() throws HoldingsItemsException {
        try {
            queueSupplier.close();
        } catch (SQLException ex) {
            log.error("Enqueue error", ex);
            throw new HoldingsItemsException("Enqueue error", ex);
        }
    }

    private static class ConsumerTarget {

        private final String consumer;
        private final Long postpone;

        private ConsumerTarget(String consumer, Long postpone) {
            this.consumer = consumer;
            this.postpone = postpone;
        }
    }

}
