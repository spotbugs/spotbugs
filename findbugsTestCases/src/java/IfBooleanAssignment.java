public class IfBooleanAssignment {
	public void test1(boolean b) {
		if (b = true) {
			System.out.println("Hi");
		}
	}

	public void test2(int a, int b, int c, int d, boolean e) {
		if (e = false) {
			System.out.println("Hi");
		}
	}

	private boolean returnTrue() {
		return true;
	}

	public void test3(boolean b) {
		if (b = returnTrue())
			System.out.println("don't report, this is more likely to be intended");
	}

	public void test4(boolean b) {
		while (b = true) {
			System.out.println("Wow");
		}
	}

	public void test5(boolean b, boolean c) {
		if (b = c)
			System.out.println("Let this go too");
	}
}
