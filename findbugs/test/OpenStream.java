import java.io.*;

public class OpenStream {
	public static void main(String[] argv) throws Exception {
		FileInputStream in = new FileInputStream(argv[0]);

		if (Boolean.getBoolean("foobar")) {
			in.close();
		}

		// oops!  exiting the method without closing the stream
	}
}
