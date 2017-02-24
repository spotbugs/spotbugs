import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class ConstantAnalysisBug {

    static SimpleDateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyyy HH:mm:ss z", Locale.US);

    protected static String formatHttpDate(Date date) {
        synchronized (httpDateFormat) {
            return httpDateFormat.format(date);
        }
    }
}
