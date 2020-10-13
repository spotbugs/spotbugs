/**
 * Class to check the issue 1254.
 */
public class Issue1254 {

	private int outerField = 0;

	private static void println(String string) {
		System.out.println(string);
	}

	public void outerMethod() {
		new Runnable() {
			@Override
			public void run() {
				++outerField;
				Inner inner = new Inner();
				++inner.innerField;
				inner.innerMethod();
				println("Anonymous class called, outerField=" + outerField);
			}
		}.run();
	}

	private void uncalledOuterMethod() {
		println("uncalledOuterMethod() should produce a warning during analysis");
	}

	private static class Inner {
		private int innerField = 0;

		private void innerMethod() {
			println("Inner.innerMethod() called, innerField=" + innerField);
		}

		private void uncalledInnerMethod() {
			println("uncalledInnerMethod() should produce a warning during analysis");
		}
	}
}
