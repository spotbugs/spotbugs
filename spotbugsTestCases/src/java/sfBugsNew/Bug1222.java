package sfBugsNew;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1222 {

        @ExpectWarning("STCAL_STATIC_CALENDAR_INSTANCE")
        public static final Calendar cal = Calendar.getInstance();
    
        @ExpectWarning("STCAL_STATIC_SIMPLE_DATE_FORMAT_INSTANCE")
        public static final DateFormat format = new SimpleDateFormat("MM");
}
