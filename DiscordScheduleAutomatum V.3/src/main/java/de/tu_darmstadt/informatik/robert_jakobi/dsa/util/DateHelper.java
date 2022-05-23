package de.tu_darmstadt.informatik.robert_jakobi.dsa.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Utility/service provider for the next 10 weekend days.
 * 
 * @author Big_Reddy
 * @since 17
 * @version 1
 */
public final class DateHelper {
    /**
     * Unused constructor
     */
    private DateHelper() {};

    /**
     * Returns the next 10 weekend days in dd.MM.yyyy format.
     * 
     * @return the next 10 weekend days in dd.MM.yyyy format
     */
    public static String[] nextWeekEnds() {
        return IntStream.range(1, 60).mapToObj(i -> {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, i);
            return cal;
        }) //
                .filter(d -> List.of(Calendar.SUNDAY, Calendar.SATURDAY).contains(d.get(Calendar.DAY_OF_WEEK))) //
                .limit(10) //
                .map(Calendar::getTime) //
                .map(new SimpleDateFormat("dd.MM.yyyy")::format) //
                .toArray(String[]::new);
    }
}
