package dk.dbc.holdingsitems.jpa;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionSort implements Comparator<String>, Serializable {

    private static final long serialVersionUID = 0xDCA11DD04A1CBC2EL;

    private static final Pattern NUMBERS = Pattern.compile("0*(0|[1-9][0-9]*)");

    private final HashMap<String, String> natural;

    public VersionSort() {
        this.natural = new HashMap<>();
    }

    @Override
    public int compare(String o1, String o2) {
        return nat(o1).compareTo(nat(o2));
    }

    private String nat(String key) {
        return natural.computeIfAbsent(key, VersionSort::naturalSortValue);
    }

    private static String naturalSortValue(String key) {
        StringBuilder value = new StringBuilder();
        int offset = 0;
        Matcher numbers = NUMBERS.matcher(key);
        while (numbers.find(offset)) {
            String group = numbers.group(1);
            value.append(key.substring(offset, numbers.start()))
                    .append('0')
                    .append((char) ( '@' + group.length() ))
                    .append(group);
            offset = numbers.end();
        }
        value.append(key.substring(offset));
        return value.toString();
    }
}
