package bugPatterns;

import java.util.Date;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class DMI_BAD_MONTH {

    @ExpectWarning("DMI_BAD_MONTH")
    void bug(Date date) {
        date.setMonth(12);
    }

    @DesireWarning("DMI_BAD_MONTH")
    void bug2(Date date) {
        boolean b = date.getMonth() == 12;
    }

}
