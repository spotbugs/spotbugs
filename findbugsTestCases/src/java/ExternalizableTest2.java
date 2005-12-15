import java.io.*;

public class ExternalizableTest2 implements Externalizable {
	ExternalizableTest2() {
	}

	public void readExternal(ObjectInput in) {
	}

	public void writeExternal(ObjectOutput out) {
	}

	static class ExternalizableTest2a extends ExternalizableTest2 {
		int x;

		ExternalizableTest2a(int i) {
			x = i;
		}

		public void readExternal(ObjectInput in) {
			x = 42;
		}

		public void writeExternal(ObjectOutput out) {
		}
	}

	static public void main(String args[]) throws Exception {
		ByteArrayOutputStream pout = new ByteArrayOutputStream();
		ObjectOutputStream oout = new ObjectOutputStream(pout);
		oout.writeObject(new ExternalizableTest2a(42));
		oout.close();
		byte b[] = pout.toByteArray();
		ByteArrayInputStream pin = new ByteArrayInputStream(b);
		ObjectInputStream oin = new ObjectInputStream(pin);
		Object o = oin.readObject();
		System.out.println("read object");
		System.out.println(o);
	}

}
