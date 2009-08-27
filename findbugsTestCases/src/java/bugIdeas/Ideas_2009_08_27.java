package bugIdeas;

import javax.annotation.CheckForNull;

public class Ideas_2009_08_27 {

	static @CheckForNull
	Object foo() {
		return System.getProperty("foo");
	}

	static void check() {

		try {
			foo().hashCode();
		} catch (Exception e) {
			assert true;
		}
	}

	static void check3() {

		try {
			foo().hashCode();
		} catch (RuntimeException e) {
			assert true;
		}
	}

	static void check2() {

		try {
			foo().hashCode();
		} catch (NullPointerException e) {
			assert true;
		}
	}

}
