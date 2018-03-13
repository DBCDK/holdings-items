/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of dbc-git:holdings-items-solr-indexer
 *
 * dbc-git:holdings-items-solr-indexer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dbc-git:holdings-items-solr-indexer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.queuebridge;

import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.QueueJob;
import dk.dbc.holdingsitems.queuebridge.monitor.JmxMetrics;
import dk.dbc.pgqueue.consumer.JobMetaData;
import dk.dbc.pgqueue.consumer.QueueWorker;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Singleton
@Startup
public class Worker {

    private static final Logger log = LoggerFactory.getLogger(Worker.class);

    @Resource(lookup = Config.DATABASE)
    DataSource dataSource;

    @Resource(type = ManagedExecutorService.class)
    ExecutorService executor;

    @Inject
    Config config;

    @Inject
    JmxMetrics metrics;

    @Inject
    JobProcessor joProcessor;

    QueueWorker worker;

    @PostConstruct
    public void init() {
        log.info("Staring worker");

        log.debug("Validating DAO version");
        try (Connection connection = dataSource.getConnection()) {
            HoldingsItemsDAO.newInstance(connection);
        } catch (HoldingsItemsException | SQLException ex) {
            throw new EJBException("Error validating dao version", ex);
        }

        worker = QueueWorker.builder()
                .consume(config.getQueues())
                .dataSource(dataSource)
                .databaseConnectThrottle(config.getDatabaseThrottle())
                .failureThrottle(config.getThrottle())
                .executor(executor)
                .emptyQueueSleep(config.getEmptyQueueSleep())
                .idleRescanEvery(config.getIdleRescanEvery())
                .maxQueryTime(config.getMaxQueryTime())
                .rescanEvery(config.getRescanEvery())
                .maxTries(config.getRetries())
                .metricRegistry(metrics.getRegistry())
                .build(QueueJob.STORAGE_ABSTRACTION, config.getThreads(), this::work);
        worker.start();
    }

    @PreDestroy
    public void destroy() {
        worker.stop();
    }

    public void work(Connection connection, QueueJob job, JobMetaData metaData) {
        log.info("Processing job: {}:{}", job.getAgencyId(), job.getBibliographicRecordId());
        try {
            joProcessor.transferJob(job, metaData.getConsumer());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
