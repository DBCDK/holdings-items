package org.w3._2001.xmlschema;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parsers {

    private static final Logger log = LoggerFactory.getLogger(Parsers.class);
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(DateTimeFormatter.ISO_DATE_TIME,
                                                                        DateTimeFormatter.ISO_DATE,
                                                                        DateTimeFormatter.BASIC_ISO_DATE,
                                                                        DateTimeFormatter.ISO_INSTANT,
                                                                        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                                                                        DateTimeFormatter.ISO_OFFSET_DATE,
                                                                        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                                                                        DateTimeFormatter.ISO_LOCAL_DATE);

    public static LocalDate parseLocalDate(String text) {
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ex) {
                log.trace("ex.getMessage() = {}", ex.getMessage());
            }
        }
        return null;
    }

    public static String printLocalDate(LocalDate date) {
        return DateTimeFormatter.ISO_DATE.format(date);
    }
}
