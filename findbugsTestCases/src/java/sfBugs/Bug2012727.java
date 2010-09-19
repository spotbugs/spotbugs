package sfBugs;

import java.text.SimpleDateFormat;

public enum Bug2012727 {
    mmddyyyy_WithTimeSlashDelimited("MM/dd/yyyy HH:mm:ss"), ddyymmM_dateOnlyNOTDelimited("ddyyMMM");

    private SimpleDateFormat simpleDateFormat;

    private Bug2012727(String format) {
        simpleDateFormat = new SimpleDateFormat(format);
    }

    public SimpleDateFormat getFormat() {
        return simpleDateFormat;
    }
}
