package obligation;

import edu.umd.cs.findbugs.annotations.NoWarning;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.WillClose;

/**
 * Test to see if WillClose annotations are understood.
 */
public class UsesWillCloseAnnotation {
	@NoWarning("OBL")
	public void ok(String filename) throws IOException {
		InputStream in = new FileInputStream(filename);
		try {
			int c;
			c = in.read();
			System.out.println(c);
		} finally {
			cleanup(in);
		}
	}
	
	
	public void cleanup(@WillClose InputStream in) {
		try {
			in.close();
		} catch (IOException e) {
			// ignore
		}
	}
}
