import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;

public class SkipBufferedInputStream2 {
	public static void main(String[] args) throws Exception {
		byte[] buffer = new byte[100];
		ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
		BufferedInputStream bis = new BufferedInputStream(bais, 50);

		byte[] smallBuf = new byte[10];
		byte[] largeBuf = new byte[50];

		bis.read(smallBuf);
		long available = bis.available();
		long r = bis.read(largeBuf);
		if (r < available && r < largeBuf.length) {
			System.out.println("Read fewer bytes than requested and fewer bytes than available");
			System.out.println("Available: " + available);
			System.out.println("Requested: " + largeBuf.length);
			System.out.println("Read: " + r);
		}
	}
}
