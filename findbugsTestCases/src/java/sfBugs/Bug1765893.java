package sfBugs;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Submitted By: Alfred Nathaniel
 * Summary:
 * 
 * The STCAL_STATIC_SIMPLE_DATE_FORMAT_INSTANCE detector is too simple minded
 * checking only the field existance but not its usage.
 */
public class Bug1765893 {
    private static final SimpleDateFormat format = new SimpleDateFormat(
            "yyyyMMdd");

    //// grep -A 1 STCAL_STATIC_SIMPLE_DATA_FORMAT_INSTANCE | grep Bug1765893
    static synchronized String formatDate(Date date) {
        return format.format(date);
    }
}
