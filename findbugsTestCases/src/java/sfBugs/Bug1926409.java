package sfBugs;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Bug1926409 {
	
	public static void foo(String filename) throws IOException {
		OutputStream os = null;
		try {
		os = new FileOutputStream(filename);

		os.write(0);
		OutputStream tmp = os;
		os = null;
		tmp.close();

		} finally {
		if (os != null) try { os.close(); } catch (IOException e) { }
		}
		}
	}


