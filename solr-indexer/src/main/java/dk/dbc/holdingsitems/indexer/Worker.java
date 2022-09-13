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

import dk.dbc.holdingsitems.QueueJob;
import dk.dbc.holdingsitems.content_dto.CompleteBibliographic;
import dk.dbc.holdingsitems.indexer.logic.JobProcessor;
import dk.dbc.pgqueue.consumer.JobMetaData;
import dk.dbc.pgqueue.consumer.QueueWorker;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.sql.DataSource;
import org.eclipse.microprofile.metrics.MetricRegistry;
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

        worker = QueueWorker.builder(QueueJob.STORAGE_ABSTRACTION)
                .skipDuplicateJobs(QueueJob.DEDUPLICATION_ABSTRACTION_IGNORE_STATECHANGE, true, true)
                .consume(config.getQueues())
                .dataSource(dataSource)
                .fromEnvWithDefaults()
                .executor(executor)
                .metricRegistryMicroProfile(metrics)
                .build(config.getThreads(), this::work);
        worker.start();
    }

    @PreDestroy
    public void destroy() {
        worker.stop();
    }

    public List<String> hungThreads() {
        return worker.hungThreads();
    }

    public void work(Connection connection, QueueJob job, JobMetaData metaData) {
        log.info("Processing job: {}:{}", job.getAgencyId(), job.getBibliographicRecordId());
        try {
            CompleteBibliographic complete = jobProcessor.getContent(job);
            if (complete != null) {
                String json = jobProcessor.buildRequestJson(complete);
                jobProcessor.putIntoSolrDocStore(job, json);
            } else {
                jobProcessor.deleteFromSolrDocStore(job);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
