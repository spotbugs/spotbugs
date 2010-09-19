package bugPatterns;

import java.util.Date;

public class DMI_BAD_MONTH {

    void bug(Date date) {
        date.setMonth(12);
    }

    void bug2(Date date) {
        boolean b = date.getMonth() == 12;
    }

}
