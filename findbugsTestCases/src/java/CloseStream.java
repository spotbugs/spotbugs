import java.io.*;

class CloseStream {
	public static void writeFile(File f, Object o) throws IOException {

		OutputStream out = new FileOutputStream(f);
		int i = o.hashCode();
		out.close();
	}

}
