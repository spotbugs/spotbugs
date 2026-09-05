package ghIssues.issue3838;

import java.time.format.DateTimeFormatter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value = "FS_BAD_DATE_FORMAT_FLAG_COMBO", justification = "Intentionally don't want to output AM/PM")
public class SuppressTimeFormatWorking {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm");
}