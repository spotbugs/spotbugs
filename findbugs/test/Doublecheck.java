public class Doublecheck {
	static private Object o;
	static private volatile Object v;
	static private String s;
	static private int i;
	static private long j;
	static private Object lock = new Object();
	

	static public Object standardDoubleCheck() {
		if (o == null) {
			synchronized (lock) {
				if (o == null)
					o = new Object();
			}
		}
		return o;
	}
	static public Object volatileDoubleCheck() {
		if (v == null) {
			synchronized (lock) {
				if (v == null)
					v = new Object();
			}
		}
		return o;
	}

	static public String stringDoubleCheck() {
		if (s == null) {
			synchronized (lock) {
				if (s == null)
					s = Thread.currentThread().toString();
			}
		}
		return s;
	}


	static public int intDoubleCheck() {
		if (i == 0) {
			synchronized (lock) {
				if (i == 0)
					i = Thread.currentThread().hashCode();
			}
		}
		return i;
	}

	static public long longDoubleCheck() {
		if (j == 0) {
			synchronized (lock) {
				if (j == 0)
					j = System.currentTimeMillis();
			}
		}
		return j;
	}




}
