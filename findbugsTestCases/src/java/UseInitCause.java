import java.io.BufferedReader;
import java.io.IOException;


public class UseInitCause {

	public String firstLine(BufferedReader r) {
		try {
			return r.readLine();
		} catch (IOException e) {
			throw (RuntimeException) new RuntimeException("IO Error").initCause(e);
		}
	}

}
