package npe;

public class DoomedNullDereference {

	public void notDoomed(boolean b) {
		Object o = null;
		if (b) {
            o = new Object();
		}
		System.out.println(o.hashCode());
	}

	public void doomed(boolean b) {
		Object o = null;
		if (b) {
            o = new Object();
		}
		System.out.println(o.hashCode());
		throw new RuntimeException();

	}

	int doomed2(Object x) {
		if (x == null)
			System.out.println("null");
        int result = x.hashCode();
		throw new RuntimeException("F" + result);
	}

}
