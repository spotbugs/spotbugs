import java.io.*;

public class TwoStreams {
	public void twoStreams() throws IOException {
		BufferedReader r = null;
		Writer w = null;

		try {
			r = new BufferedReader(new InputStreamReader(new FileInputStream("hello")));
			String l = r.readLine();
			w = new OutputStreamWriter(new FileOutputStream("blah"));
			w.write(l);
		} finally {
			if (r != null) {
				r.close();
			}
			if (w != null) {
				try {
					w.close();
				} catch (IOException e) {
				}
			}
		}
	}
}

// vim:ts=4
