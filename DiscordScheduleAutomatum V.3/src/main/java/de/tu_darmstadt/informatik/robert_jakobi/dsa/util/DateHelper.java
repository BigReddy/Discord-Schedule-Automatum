package de.tu_darmstadt.informatik.robert_jakobi.dsa.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Utility/service provider for the next 10 weekend days.
 * 
 * @author Big_Reddy
 * @since 17
 * @version 2
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
        return Stream.generate(new Supplier<Calendar>() {
            Calendar base = Calendar.getInstance();

            public Calendar get() {
                this.base.add(Calendar.DAY_OF_YEAR, 1);
                return this.base;
            }
        }) //
                .filter(d -> List.of(Calendar.SUNDAY, Calendar.SATURDAY).contains(d.get(Calendar.DAY_OF_WEEK))) //
                .limit(10) //
                .map(Calendar::getTime) //
                .map(Date::toInstant) //
                .map(DateFormat.DATE_DE::format) //
                .toArray(String[]::new);
    }

    public static String dateAt(LocalDate date, String timeString, DateFormat format) {
        var time = LocalTime.parse(timeString);
        var ldt = date.atTime(time);
        return format.format(ldt);
    }
}
