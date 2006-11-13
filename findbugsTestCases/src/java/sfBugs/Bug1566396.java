package sfBugs;

/**

Findbugs will complain with "Field not initialized in
constructor" (UwF) when the following static
initializer pattern (seen several times) is used:


Is it possible not to issue the warning if static
fields are initialized in the static initializer (or a
method called from the static initializer)?
 *
 */
public class Bug1566396 {
	private static String[] array;

	static {
		reset();
	}

	private Bug1566396() {
	}

	int foo() {
		return array.length;
	}
	 static void reset() {
		array = new String[0];
	}
}
