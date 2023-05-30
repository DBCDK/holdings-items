package org.w3._2001.xmlschema;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ParsersTest {

    @Test
    public void testLocalDate() throws Exception {
        System.out.println("testLocalDate");
        assertThat(Parsers.parseLocalDate("2023-05-30+02:00"), notNullValue());
        assertThat(Parsers.parseLocalDate("2023-05-08Z"), notNullValue());
    }

    @Test
    public void testInstant() throws Exception {
        System.out.println("testInstant");
        assertThat(Parsers.parseInstant("2023-05-30T10:34:04.567+02:00"), notNullValue());
    }
}
