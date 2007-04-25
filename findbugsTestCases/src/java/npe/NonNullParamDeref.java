package npe;

import edu.umd.cs.findbugs.annotations.NonNull;

public class NonNullParamDeref {
	static void foo(@NonNull Object o) {

	}

	void bar(@NonNull Object o) {

	}

	int count;

	void report1(boolean a) {

		Object x = null;
		if (a) {
			x = new Object();
		}

		foo(x);
	}

	void report2(boolean b) {
		Object x = null;

		if (b) {
			x = new Object();
		}

		bar(x);
	}

}
