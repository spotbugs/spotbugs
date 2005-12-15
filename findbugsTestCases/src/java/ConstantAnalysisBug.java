import java.text.*;
import java.util.*;

class ConstantAnalysisBug {

	static SimpleDateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyyy HH:mm:ss z",
			Locale.US);

	protected static String formatHttpDate(Date date) {
		synchronized (httpDateFormat) {
			return httpDateFormat.format(date);
		}
	}
}
