public class Doublecheck {
	public Object o;

	public void t() {
		if (o == null) {
			synchronized (this) {
				if (o == null)
					o = new Object();
			}
		}
	}
}
