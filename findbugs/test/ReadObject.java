import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

class NotSerializable {
	public synchronized void readObject(ObjectInputStream in) throws IOException {
	}
}


public class ReadObject implements Serializable {
	public synchronized void readObject(ObjectInputStream in) throws IOException {
	}

}
