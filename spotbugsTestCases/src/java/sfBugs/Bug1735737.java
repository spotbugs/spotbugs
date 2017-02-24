package sfBugs;

import java.util.Calendar;
import java.util.TimeZone;

public abstract class Bug1735737 {
    abstract TimeZone getTimeZone();

    Calendar calendar = Calendar.getInstance();

    Calendar subParseZoneString(String text, int j) {
        TimeZone tz = getTimeZone();
        calendar.set(Calendar.DST_OFFSET, j >= 3 ? tz.getDSTSavings() : 0);
        return calendar;

    }
}
