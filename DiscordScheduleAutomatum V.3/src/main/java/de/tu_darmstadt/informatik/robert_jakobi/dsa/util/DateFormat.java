package de.tu_darmstadt.informatik.robert_jakobi.dsa.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

public enum DateFormat {
    FILE_FULL("yyyy-MM-dd_HH-mm-ss-SSS"),
    ICAL_DATE("yyyyMMdd"),
    ICAL_DATE_FULL("yyyyMMdd'T'HHmmss"),
    DATE_DE("dd.MM.yyyy"),
    DATE_DE_FILE("dd_MM_yyyy"), //
    ;

    private final DateTimeFormatter format;

    private DateFormat(String format) {
        this.format = DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault());
    }

    public TemporalAccessor parse(String dateString) {
        try {
            return this.format.parse(dateString);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public String format(TemporalAccessor date) {
        return this.format.format(date);
    }
}
