package sfBugs;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug3441912 {
    @ExpectWarning("STCAL_STATIC_SIMPLE_DATE_FORMAT_INSTANCE")
    public static final SimpleDateFormat FORMAT_DB_DATE = new SimpleDateFormat("yyyyMMdd");

    @ExpectWarning("STCAL_STATIC_SIMPLE_DATE_FORMAT_INSTANCE")
    private static  DateFormat formatDBDate2;

    
    public static void setFormat(DateFormat f) {
        formatDBDate2 = f;
    }
    
    @ExpectWarning("STCAL_INVOKE_ON_STATIC_DATE_FORMAT_INSTANCE")
    public String one() {
        return FORMAT_DB_DATE.format("");
    }
    
    @ExpectWarning("STCAL_INVOKE_ON_STATIC_DATE_FORMAT_INSTANCE")
    public String two() {
        return  formatDBDate2.format("");
    }
    
    
    
}
