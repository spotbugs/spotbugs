import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

class PublicReadObject implements Serializable {
	transient int x;

	int y;

	public void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		x = ois.readInt();
	}
}
