package dk.dbc.holdingsitems.jpa;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class VersionSortTest {

    @Test
    public void testCompare() {
        assertThat(sorted("abc", "1", "10", "9.3", "9.10"), contains("1", "9.3", "9.10", "10", "abc"));
    }

    private List<String> sorted(String... elems) {
        return Stream.of(elems)
                .sorted(new VersionSort())
                .collect(Collectors.toList());
    }
}
