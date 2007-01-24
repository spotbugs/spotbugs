import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class TwoStreams {
	int x;

	public void twoStreamsWrong() throws IOException {
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

	public void nullDereferenceCheck(TwoStreams o) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream("hello")));
		int i = o.x;
		r.close();
	}

	public void twoStreamsRight() throws IOException {
		BufferedReader r = null;
		Writer w = null;

		try {
			r = new BufferedReader(new InputStreamReader(new FileInputStream("hello")));
			String l = r.readLine();
			w = new OutputStreamWriter(new FileOutputStream("blah"));
			w.write(l);
		} finally {
			if (w != null) {
				try {
					w.close();
				} catch (IOException e) {
				}
			}
			if (r != null) {
				r.close();
			}
		}
	}
}
