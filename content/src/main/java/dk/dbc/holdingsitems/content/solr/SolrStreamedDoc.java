package dk.dbc.holdingsitems.content.solr;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.apache.solr.client.solrj.io.Tuple;

public class SolrStreamedDoc {

    private final Map<String, Object> doc;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SolrStreamedDoc(Tuple doc) {
        this.doc = doc.getFields();
    }

    public String getValue(SolrFields field) {
        Object obj = doc.get(field.getFieldName());
        if (obj == null)
            throw new IllegalStateException("Document does not have a " + field.getFieldName() + " value");
        if (obj instanceof Collection)
            return String.valueOf(( (Collection) obj ).iterator().next());
        return String.valueOf(obj);
    }

    public Collection<String> getValues(SolrFields field) {
        Object obj = doc.get(field.getFieldName());
        if (obj == null)
            throw new IllegalStateException("Document does not have a " + field.getFieldName() + " value");
        if (obj instanceof Collection)
            return (Collection) obj;
        return Collections.singleton(String.valueOf(obj));
    }

    public Collection<String> getValuesOptional(SolrFields field) {
        Object obj = doc.get(field.getFieldName());
        if (obj == null)
            return Collections.emptyList();
        if (obj instanceof Collection)
            return (Collection) obj;
        return Collections.singleton(String.valueOf(obj));
    }
}
