import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

class NotSerializable {
	private synchronized void readObject(ObjectInputStream in) throws IOException {
	}
}

public class ReadObject implements Serializable {
	private synchronized void readObject(ObjectInputStream in) throws IOException {
	}

	private synchronized void writeObject(ObjectOutputStream in) throws IOException {
	}

}
