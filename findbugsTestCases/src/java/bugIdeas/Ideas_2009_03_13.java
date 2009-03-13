package bugIdeas;

public class Ideas_2009_03_13 {

	static void checkNotNull(Object o) {
		if (o == null)
			throw new NullPointerException();
		return;
	}

	static int foo(Object x) {
		checkNotNull(x);
		if (x == null)
			return x.hashCode();
		return 42;
	}

	static int foo2(Object x) {
		if (x == null) {
			checkNotNull(x);
			return x.hashCode();
		}
		return 42;
	}

}
