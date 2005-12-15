package nullnessAnnotations;

import edu.umd.cs.findbugs.annotations.*;

public class TestNonNull4 extends TestNonNull3 {

	Object f(Object o) {
		return o;
	}

	Object baz() {
		return f(null);
	}
}
