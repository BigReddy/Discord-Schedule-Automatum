package de.tu_darmstadt.informatik.robert_jakobi.dsa.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

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
        try {
            template = Files.readString(Paths.get("rec", "template.ical"));
            defaultData = Files.lines(Paths.get("rec", "invite.data")).toArray(String[]::new);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
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
     * @param data
     *            Additional data for the .ics file
     * @return InputStream of the contents of an .ics file
     */
    public static InputStream getICal(String date, String... data) {
        final String date_string;
        try {
            date_string = new SimpleDateFormat("yyyyMMdd").format(new SimpleDateFormat("dd.MM.yyyy").parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        if (data.length == 0) data = defaultData;
        return new ByteArrayInputStream(template.formatted( //
                System.currentTimeMillis(), //
                new SimpleDateFormat("yyyyMMdd-HHmmss").format(Calendar.getInstance().getTime()).replace('-', 'T'), //
                date_string, "1300", //
                date_string, "1800", //
                data[0], data[1], data[2]) //
                .getBytes());
    }
}
