package ghIssues;

import java.time.format.DateTimeFormatter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Issue3838 {
    public static class SuppressTimeFormatBroken {

        @SuppressFBWarnings(value = "FS_BAD_DATE_FORMAT_FLAG_COMBO", justification = "Intentionally don't want to output AM/PM")
        private static final DateTimeFormatter TIME_FORMAT_SUPPRESSED = DateTimeFormatter.ofPattern("hh:mm");
        
        private static final DateTimeFormatter TIME_FORMAT_NOT_SUPPRESSED = DateTimeFormatter.ofPattern("hh:mm");

        @SuppressFBWarnings(value = "FS_BAD_DATE_FORMAT_FLAG_COMBO", justification = "Intentionally don't want to output AM/PM")
        private final DateTimeFormatter timeFormatSuppressed = DateTimeFormatter.ofPattern("hh:mm");
        
        private final DateTimeFormatter timeFormatNotSuppressed = DateTimeFormatter.ofPattern("hh:mm");
    }

    @SuppressFBWarnings(value = "FS_BAD_DATE_FORMAT_FLAG_COMBO", justification = "Intentionally don't want to output AM/PM")
    public static class SuppressTimeFormatWorking {

        private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm");
    }
}
