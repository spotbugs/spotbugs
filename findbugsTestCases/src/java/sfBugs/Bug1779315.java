package sfBugs;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Bug1779315 {
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	private static final DateFormat alternativeDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss");

}
