class IntHolder {
	int value;
}

public class Ser implements java.io.Serializable {
	private IntHolder holder = new IntHolder();

	public void set(int v) {
		holder.value = v;
	}
}
