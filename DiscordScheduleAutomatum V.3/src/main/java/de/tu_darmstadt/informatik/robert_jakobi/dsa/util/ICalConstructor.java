package de.tu_darmstadt.informatik.robert_jakobi.dsa.util;

import static de.tu_darmstadt.informatik.robert_jakobi.dsa.util.DateFormat.ICAL_DATE_FULL;
import static de.tu_darmstadt.informatik.robert_jakobi.dsa.util.FileManager.ICAL_DATA_FILE;
import static de.tu_darmstadt.informatik.robert_jakobi.dsa.util.FileManager.ICAL_FORMAT_FILE;
import static de.tu_darmstadt.informatik.robert_jakobi.dsa.util.FileManager.loadFromFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAccessor;
import java.util.UUID;

/**
 * Utility/service provider for generating .ics files of given template.
 * 
 * @author Big_Reddy
 * @since 11
 * @version 3
 */
public class ICalConstructor {
    /**
     * Template of the .ics
     */
    private final static String template;

    private final static String[] defaultData;

    static {
        template = loadFromFile(ICAL_FORMAT_FILE);
        defaultData = loadFromFile(ICAL_DATA_FILE) //
                .lines() //
                .toArray(String[]::new);
    }

    /**
     * Unused constructor
     */
    private ICalConstructor() {}

    /**
     * Generates a stream representing the content of an .ics file derived from
     * given values.
     * 
     * @param date
     *            Date of the event
     * @param inputData
     *            Additional data for the .ics file
     * @return InputStream of the contents of an .ics file
     */
    public static InputStream getICal(TemporalAccessor date, UUID uuid, String... inputData) {
        var localDate = LocalDate.from(date);
        String[] data = inputData.length == 0 ? defaultData : inputData;
        return new ByteArrayInputStream(template.formatted( //
                uuid.toString(), //
                ICAL_DATE_FULL.format(Instant.now()), //
                DateHelper.dateAt(localDate, data[3], ICAL_DATE_FULL), //
                DateHelper.dateAt(localDate, data[4], ICAL_DATE_FULL), //
                data[2], data[1], data[0], data[0]) //
                .getBytes());
    }
}
