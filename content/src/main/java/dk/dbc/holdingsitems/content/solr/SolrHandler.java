package dk.dbc.holdingsitems.content.solr;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.stream.expr.DefaultStreamFactory;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionNamedParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamFactory;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.dbc.holdingsitems.content.solr.SolrFields.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
public class SolrHandler {

    private static final Logger log = LoggerFactory.getLogger(SolrHandler.class);

    private static final String FIELDS =
            String.join(", ",
                        HOLDINGSITEM_ID.getFieldName(),
                        HOLDINGSITEM_BIBLIOGRAPHIC_RECORD_ID.getFieldName(),
                        HOLDINGSITEM_STATUS_FOR_STREAMING.getFieldName(),
                        HOLDINGSITEM_ITEM_ID.getFieldName());
    private static final String SOLR_SORT_BY_ID =
            HOLDINGSITEM_ID.getFieldName() + " asc";

    private static final Pattern ZK = Pattern.compile("zk://(([^/]*)(/.*)?)/([^/]*)");
    private static final int ZK_URL = 1;
    private static final int ZK_HOSTS = 2;
    private static final int ZK_CHROOT = 3;
    private static final int ZK_COLLECTION = 4;

    private static final SolrClientCache SOLR_CLIENT_CACHE = new SolrClientCache();

    @Inject
    @ConfigProperty(name = "COREPO_SOLR_URL")
    private String solrUrl;

    private String collection;
    private StreamFactory streamFactory;
    private CloudSolrClient solrClient;

    public SolrHandler() {
    }

    public SolrHandler(String solrUrl) {
        this.solrUrl = solrUrl;
    }

    @PostConstruct
    public void init() {
        Matcher zkMatcher = ZK.matcher(solrUrl);
        if (!zkMatcher.find())
            throw new IllegalArgumentException("Cannot parse solrUrl");

        this.collection = zkMatcher.group(ZK_COLLECTION);

        this.streamFactory = new DefaultStreamFactory()
                .withCollectionZkHost(collection,
                                      zkMatcher.group(ZK_URL));

        Optional<String> zkChroot = Optional.empty();
        if (zkMatcher.group(ZK_CHROOT) != null) {
            zkChroot = Optional.of(zkMatcher.group(ZK_CHROOT));
        }
        List<String> zkHosts = Arrays.asList(zkMatcher.group(ZK_HOSTS).split(","));

        this.solrClient = new CloudSolrClient.Builder(zkHosts, zkChroot)
                .build();
        this.solrClient.setDefaultCollection(collection);
    }

    public void loadAgencyHoldings(int agencyId, Consumer<SolrStreamedDoc> consumer) throws IOException {
        streamRequest("holdingsitem.agencyId:" + agencyId, consumer);
    }

    public void loadPidHoldings(int agencyId, String pid, Consumer<SolrStreamedDoc> consumer) throws IOException {
        streamRequest("holdingsitem.agencyId:" + agencyId + " AND rec.repositoryId:" + ClientUtils.escapeQueryChars(pid), consumer);
    }

    private void streamRequest(String q, Consumer<SolrStreamedDoc> consumer) throws IOException {
        TupleStream stream = streamFactory.constructStream(new StreamExpression("search")
                .withParameter(collection)
                .withParameter(new StreamExpressionNamedParameter("qt", "/export"))
                .withParameter(new StreamExpressionNamedParameter("appId", "holdings-items-content-service"))
                .withParameter(new StreamExpressionNamedParameter("q", q))
                .withParameter(new StreamExpressionNamedParameter("fl", FIELDS))
                .withParameter(new StreamExpressionNamedParameter("sort", SOLR_SORT_BY_ID)));
        stream.setStreamContext(streamContext());

        log.debug("Requesting solr documents");
        stream.open();
        try {
            for (;;) {
                Tuple t = stream.read();
                if (t.EOF)
                    break;
                if (t.EXCEPTION) {
                    log.error("Error from SolR: {}", t.getException());
                } else {
                    consumer.accept(new SolrStreamedDoc(t));
                }
            }
        } finally {
            log.debug("Completed solr documents");
            stream.close();
        }
    }

    private static StreamContext streamContext() {
        StreamContext context = new StreamContext();
        context.setSolrClientCache(SOLR_CLIENT_CACHE);
        return context;
    }
}
