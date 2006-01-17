import java.io.Serializable;

public class BadStoreOfNonSerializableObject implements Serializable{

	private static final long serialVersionUID = 0;
	Object x;
	NotSerializable y;
	static final class NotSerializable {}
	
	BadStoreOfNonSerializableObject() {
		x = new NotSerializable();
		y = new NotSerializable();
	}

}
