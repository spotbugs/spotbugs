package npe;

public class LoadKnownNull {

	static void simple() {
		Object o = null;
		System.out.println(o);
	}

	/** false positive NP_LOAD_OF_KNOWN_NULL_VALUE */
	static void lenbok(String s) {
		try {
			if (s == null) System.out.println("s is null");
		} finally {
			System.out.println(s); //false+ NP_LOAD_OF_KNOWN_NULL_VALUE
		}
	}

	/** false positive NP_LOAD_OF_KNOWN_NULL_VALUE */
	static void dmoellen() {
		Object foo = null;
		try {
			foo = new Object();
		} finally {
			System.out.println(foo); //false+ NP_LOAD_OF_KNOWN_NULL_VALUE
		}
	}

}
