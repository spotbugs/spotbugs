import java.util.*;

class BadCast {
	
	public static <C extends Collection<?>> C smallest(Iterable<C> collections) {
		return null;
	}
	
	public static int sizeOfSmallest(Iterable<? extends Set<?>> sets) {
		// TODO: False positive BC here
		Set<?> s = smallest(sets);
		return s.size();
	}
	List a;

	public Vector swap(List b) {
		Vector v = (Vector) a;
		a = (Vector) b;
		return v;
	}

	Object foo() {
		return new Hashtable();
	}

	Map bar() {
		return new Hashtable();
	}

	Vector[] faz() {
		return new Vector[10];
	}

	Hashtable baz() {
		return new Hashtable();
	}

	int d() {
		Map m = bar();
		Set s = (Set) m.values();
		return s.size();
	}

	int f() {
		return ((Hashtable) foo()).size();
	}

	int f2() {
		Object o = faz();
		return ((Hashtable[]) o).length;
	}

	int h() {
		return ((Hashtable) bar()).size();
	}

	int h2() {
		Map m = bar();
		if (m instanceof Hashtable)
			return ((Hashtable) m).size();
		return 17;
	}

	int g() {
		return ((Hashtable[]) foo()).length;
	}

	int hx() {
		Object o = baz();
		try {
			if (o instanceof Collection) {
				System.out.println("Yeah..." + ((Set) o).size());
			}
			if (o instanceof Stack)
				System.out.println("Strange...");
			else if (o instanceof Map)
				return ((Map) o).size();
			return ((Vector) o).size();
		} finally {
			if (o instanceof Map)
				System.out.println("Cool");
		}

	}

	static Object f(boolean b, Integer i, String s) {
		return b ? (Integer) i : (String) s;
	}
}
