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
package dk.dbc.holdingsitems.indexer.logic;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.holdingsitems.QueueJob;
import dk.dbc.holdingsitems.content_dto.CompleteBibliographic;
import dk.dbc.holdingsitems.indexer.Config;
import dk.dbc.pgqueue.consumer.NonFatalQueueError;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeExecutor;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Stateless
public class JobProcessor {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(JobProcessor.class);

    private static final ObjectMapper O = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static final FailsafeExecutor<CompleteBibliographic> RETRIEVE_DELAY = Failsafe.with(new RetryPolicy<CompleteBibliographic>()
            .abortOn(JobProcessor::notFoundButEndpointHasHandled)
            .withBackoff(1, 5, ChronoUnit.SECONDS)
            .withJitter(.5)
            .withMaxAttempts(3));

    private static final FailsafeExecutor<String> SEND_DELAY = Failsafe.with(new RetryPolicy<String>()
            .withBackoff(1, 5, ChronoUnit.SECONDS)
            .withJitter(.5)
            .withMaxAttempts(3));

    private static boolean notFoundButEndpointHasHandled(Throwable t) {
        return t instanceof NotFoundException &&
               ( (NotFoundException) t ).getResponse().getHeaderString("X-DBC-Status") != null;
    }

    private Client client;
    private UriBuilder solrDocStoreUri;
    private UriBuilder contentServiceUri;

    @Inject
    Config config;

    public JobProcessor() {
    }

    public JobProcessor(Config config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        client = config.getClient();
        solrDocStoreUri = UriBuilder.fromUri(config.getSolrDocStoreUrl())
                .path("{agencyId}-{bibliographicRecordId}").queryParam("trackingId", "{trackingId}");
        contentServiceUri = UriBuilder.fromUri(config.getHoldingsItemsContentServiceUrl())
                .path("{agencyId}").path("{bibliographicRecordId}").queryParam("trackingId", "{trackingId}");
    }

    @Timed
    public CompleteBibliographic getContent(QueueJob job) throws NonFatalQueueError {
        URI uri = contentServiceUri.buildFromMap(Map.of("agencyId", job.getAgencyId(),
                                                        "bibliographicRecordId", job.getBibliographicRecordId(),
                                                        "trackingId", job.getTrackingId()));
        log.debug("fetching: {}", uri);
        try {
            return RETRIEVE_DELAY.get(() -> {
                try (InputStream is = client
                        .target(uri)
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get(InputStream.class)) {
                    return O.readValue(is, CompleteBibliographic.class);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (RuntimeException ex) {
            if (notFoundButEndpointHasHandled(ex)) {
                return null;
            }
            throw new NonFatalQueueError("Error getting content for: " + uri + " " + ex.getMessage(), ex);
        }
    }

    @Timed
    public String buildRequestJson(CompleteBibliographic complete) throws Exception {
        ItemGrouping grouping = new ItemGrouping();
        ItemMerge bibliographicData = new ItemMerge()
                .with(SolrFields.AGENCY_ID.getFieldName(), String.valueOf(complete.agencyId))
                .with(SolrFields.BIBLIOGRAPHIC_RECORD_ID.getFieldName(), complete.bibliographicRecordId)
                .with(SolrFields.FIRST_ACCESSION_DATE.getFieldName(), complete.firstAccessionDate)
                .with(SolrFields.NOTE.getFieldName(), complete.note)
                .with(SolrFields.REC_BIBLIOGRAPHIC_RECORD_ID.getFieldName(), complete.bibliographicRecordId);
        complete.issues.forEach(issue -> {
            ItemMerge issueData = bibliographicData.deepCopy()
                    .with(SolrFields.EXPECTED_DELIVERY.getFieldName(), issue.expectedDelivery)
                    .with(SolrFields.ISSUE_ID.getFieldName(), issue.issueId)
                    .with(SolrFields.ISSUE_TEXT.getFieldName(), issue.issueText)
                    .with(SolrFields.READY_FOR_LOAN.getFieldName(), issue.readyForLoan);
            issue.items.forEach(item -> {
                ItemMerge itemData = issueData.deepCopy()
                        .with(SolrFields.ACCESSION_DATE.getFieldName(), item.accessionDate)
                        .with(SolrFields.BIBLIOGRAPHIC_RECORD_ID.getFieldName(), item.bibliographicRecordId)
                        .with(SolrFields.BRANCH.getFieldName(), item.branch)
                        .with(SolrFields.BRANCH_ID.getFieldName(), item.branchId)
                        .with(SolrFields.CIRCULATION_RULE.getFieldName(), item.circulationRule)
                        .with(SolrFields.DEPARTMENT.getFieldName(), item.department)
                        .with(SolrFields.ITEM_ID.getFieldName(), item.itemId)
                        .with(SolrFields.LOAN_RESTRICTION.getFieldName(), item.loanRestriction)
                        .with(SolrFields.LOCATION.getFieldName(), item.location)
                        .with(SolrFields.STATUS.getFieldName(), item.status)
                        .with(SolrFields.SUBLOCATION.getFieldName(), item.subLocation);
                grouping.add(itemData);
            });
        });
        return O.writeValueAsString(Map.of("indexKeys", grouping.documents()));
    }

    @Timed
    public void putIntoSolrDocStore(QueueJob job, String json) throws NonFatalQueueError {
        URI uri = solrDocStoreUri.buildFromMap(Map.of("agencyId", job.getAgencyId(),
                                                      "bibliographicRecordId", job.getBibliographicRecordId(),
                                                      "trackingId", job.getTrackingId()));
        log.debug("putting: {}", uri);
        try {
            SEND_DELAY.get(() ->
                    client.target(uri)
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .put(Entity.json(json), String.class));
        } catch (RuntimeException ex) {
            throw new NonFatalQueueError("Error sending content to: " + uri + " " + ex.getMessage(), ex);
        }
    }

    @Timed
    public void deleteFromSolrDocStore(QueueJob job) throws NonFatalQueueError {
        URI uri = solrDocStoreUri.buildFromMap(Map.of("agencyId", job.getAgencyId(),
                                                      "bibliographicRecordId", job.getBibliographicRecordId(),
                                                      "trackingId", job.getTrackingId()));
        log.debug("deleting: {}", uri);
        try {
            SEND_DELAY.get(() ->
                    client.target(uri)
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .delete(String.class));
        } catch (RuntimeException ex) {
            throw new NonFatalQueueError("Error sending content to: " + uri + " " + ex.getMessage(), ex);
        }
    }
}
