package sfBugs;

public class Bug1995271 {

	public static int foo(int x, int y) {
		if (x < 5 || x < 5)
			throw new IllegalArgumentException("This is wrong");
		return x + y;
	}

	public static int foo2(int x, int y) {
		if (x < 5 && x < 5)
			throw new IllegalArgumentException("This is wrong");
		return x + y;
	}

}
