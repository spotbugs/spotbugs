package sfBugs;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Bug1779315 {
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	private static final DateFormat alternativeDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss");

	public void testFilesMethods() throws IOException {
	    File f = new File("blah.txt");
        File f2 = new File("blah2.txt");
        
        // All of these should generate a warning
	    f.mkdir();
        f.mkdirs();
        f.delete();
        f.createNewFile();
        f.setLastModified(1L);
        f.setReadOnly();
        f.renameTo(f2);
	}
}
