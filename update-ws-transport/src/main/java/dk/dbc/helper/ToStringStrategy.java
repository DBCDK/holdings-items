package dk.dbc.helper;

import java.util.Collection;
import java.util.Iterator;
import org.jvnet.basicjaxb.lang.DefaultToStringStrategy;
import org.jvnet.basicjaxb.locator.ObjectLocator;
import org.jvnet.basicjaxb.locator.util.LocatorUtils;

public class ToStringStrategy extends DefaultToStringStrategy {

    @Override
    protected void appendContentEnd(StringBuilder buffer) {
        buffer.append('}');
    }

    @Override
    protected void appendContentStart(StringBuilder buffer) {
        buffer.append('{');
    }

    @Override
    protected void appendArrayEnd(StringBuilder buffer) {
        buffer.append(']');
    }

    @Override
    protected void appendArrayStart(StringBuilder buffer) {
        buffer.append('[');
    }

    @Override
    protected void appendNullText(StringBuilder buffer) {
        buffer.append("null");
    }

    @Override
    public boolean isUseIdentityHashCode() {
        return false;
    }

    @Override
    public boolean isUseFieldNames() {
        return true;
    }

    @Override
    public boolean isUseDefaultFieldValueMarkers() {
        return false;
    }

    @Override
    protected StringBuilder append(ObjectLocator locator, StringBuilder buffer, Collection array) {
        this.appendArrayStart(buffer);
        int pos = 0;
        for (Iterator iterator = array.iterator() ; iterator.hasNext() ;) {
            this.append(LocatorUtils.item(locator, pos++, array), buffer, iterator.next());
            if (iterator.hasNext())
                this.appendArraySeparator(buffer);
        }
        this.appendArrayEnd(buffer);
        return buffer;
    }

    @Override
    public StringBuilder append(ObjectLocator locator, StringBuilder buffer, Object value) {
        if (value != null && value instanceof String) {
            return buffer.append('"')
                    .append(( (String) value ).replaceAll("([\"\\\\])", "\\\\$1"))
                    .append('"');
        } else {
            return super.append(locator, buffer, value);
        }
    }
}
