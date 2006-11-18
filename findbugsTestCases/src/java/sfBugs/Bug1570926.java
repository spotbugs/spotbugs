package sfBugs;

public class Bug1570926 {
	static class MySubClass extends Bug1570926 {
	};

	public static void doNotReport(Bug1570926 anItem) {
		if (anItem == null || anItem instanceof MySubClass) {
			doIt((MySubClass) anItem);
		} else
			throw new IllegalArgumentException();
	}

	private static void doIt(MySubClass class1) {

	}

}
