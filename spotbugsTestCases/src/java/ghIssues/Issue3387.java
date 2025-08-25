package ghIssues;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Issue3387 {

    public String testDateFormat() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy à HH'h'mm", Locale.FRENCH);
        return ZonedDateTime.now(ZoneId.systemDefault()).format(formatter);
    }
}
