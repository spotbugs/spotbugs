package sfBugs;

public class Bug1570926 {

	public static void doNotReport(Object anItem) {
		if (anItem == null || anItem instanceof String) {
			doIt((String) anItem);
		} else
			throw new IllegalArgumentException();
	}

	private static void doIt(String class1) {

	}

}
