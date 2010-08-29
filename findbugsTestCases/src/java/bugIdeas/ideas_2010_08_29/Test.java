package bugIdeas.ideas_2010_08_29;

public class Test {
	
	private void test(Object x) {
		
	}

	private class X {
		X(Object o) {}
		public int foo(Object x) { return 17; }
		
	}
	private static class Y {
		Y(Object o) {}
		static void test (Object x) {}
		public int foo(Object x) { return 17; }
		
	}
	
	public static void main(String args[]) {
		Test test = new Test();
		test.test(null);
		X x = test.new X(null);
		x.foo(null);
		Y y = new Y(null);
		y.foo(null);
		Y.test(null);
	}
}
