package sfBugs;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug3441912 {
    @ExpectWarning("STCAL_STATIC_SIMPLE_DATE_FORMAT_INSTANCE")
    public static final SimpleDateFormat FORMAT_DB_DATE = new SimpleDateFormat("yyyyMMdd");

    @ExpectWarning("STCAL_STATIC_SIMPLE_DATE_FORMAT_INSTANCE")
    public static final DateFormat FORMAT_DB_DATE2 = new SimpleDateFormat("yyyyMMdd");

    
    @ExpectWarning("STCAL_STATIC_SIMPLE_DATE_FORMAT_INSTANCE")
    public static  DateFormat formatDBDate3;

    
    public static void setFormat(DateFormat f) {
        formatDBDate3 = f;
    }
    
    @ExpectWarning("STCAL_INVOKE_ON_STATIC_DATE_FORMAT_INSTANCE")
    public String one() {
        return FORMAT_DB_DATE.format("");
    }
    
    @ExpectWarning("STCAL_INVOKE_ON_STATIC_DATE_FORMAT_INSTANCE")
    public String two() {
        return  FORMAT_DB_DATE2.format("");
    }
    
    
    @ExpectWarning("STCAL_INVOKE_ON_STATIC_DATE_FORMAT_INSTANCE")
    public String three() {
        return  formatDBDate3.format("");
    }
    
    
}
