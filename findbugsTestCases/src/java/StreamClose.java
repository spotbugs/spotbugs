import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class StreamClose {
	public void frotz(String fileName) throws IOException {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(fileName);
			writeXML(out);
			out.close();
		} catch (IOException ioe) {
			// Stuff to handle catch; does not use "out" at all.
		} finally {
			// ******** Following line reported as "Useless Control Flow"
			// ********
			if (out != null)
				out.close();
		}

	}

	public abstract void writeXML(OutputStream out) throws IOException;
}

// vim:ts=4
