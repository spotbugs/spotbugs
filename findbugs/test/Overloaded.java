public class Overloaded {
	public void foo(int x) {
		System.out.println(new Boolean(true));
	}

	public void foo(String s) {
		System.out.println(new Boolean(true));
	}

	public void foo(String s, boolean y) {
		System.out.println(new Boolean(true));
	}
}
