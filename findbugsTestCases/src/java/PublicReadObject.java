import java.io.*;

class PublicReadObject implements Serializable {
	transient int x;

	int y;

	public void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		x = ois.readInt();
	}
}
