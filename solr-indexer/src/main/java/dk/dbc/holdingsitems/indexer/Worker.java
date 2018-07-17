/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items
 *
 * holdings-items is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.indexer;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.QueueJob;
import dk.dbc.holdingsitems.indexer.logic.JobProcessor;
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
    JobProcessor jobProcessor;

    @Inject
    MetricRegistry metrics;

    private QueueWorker worker;

    @PostConstruct
    public void init() {
        log.info("Staring worker");

        log.debug("Validating DAO version");
        try (Connection connection = dataSource.getConnection()) {
            HoldingsItemsDAO.newInstance(connection);
        } catch (HoldingsItemsException | SQLException ex) {
            throw new EJBException("Error validating dao version", ex);
        }

        worker = QueueWorker.builder(QueueJob.STORAGE_ABSTRACTION)
                .skipDuplicateJobs(QueueJob.DEDUPLICATION_ABSTRACTION_IGNORE_STATECHANGE)
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
                .metricRegistry(metrics)
                .build(config.getThreads(), this::work);
        worker.start();
    }

    @PreDestroy
    public void destroy() {
        worker.stop();
    }

    public void work(Connection connection, QueueJob job, JobMetaData metaData) {
        log.info("Processing job: {}:{}", job.getAgencyId(), job.getBibliographicRecordId());
        try {
            ObjectNode json = jobProcessor.buildRequestJson(connection, job);
            JsonNode response = jobProcessor.sendToSolrDocStore(json);
            if (response.get("status").asInt(-1) != 200) {
                String status = response.get("status-text").asText("Unknown response code");
                log.error("Communication failure: {}", status);
                String body = response.get("body").asText("Unknown response body");
                log.debug("Communication failure: ", body);
                throw new RuntimeException(status);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
