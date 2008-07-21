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

	public static int foo3(int x, int y) {
		if (x < 5)
			if (x < 5)
				throw new IllegalArgumentException("This is wrong");
		return x + y;
	}

	public static boolean foo4(int x, int y) {
		if (x == 3 || x != 3 && y == 5)
			return true;
		return false;
	}

}
