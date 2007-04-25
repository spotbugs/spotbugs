package nullnessAnnotations;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

public class TestFieldAnnotations {
	@NonNull Object x;
	@CheckForNull Object y;

	void f() {
		x = y;
	}
	void g() {
		y = x;
	}
	void h() {
		if (x == null) System.out.println("Huh?");
	}

	void i() {
		if (y != null) {
			System.out.println(y.hashCode());
		}
	}

}
