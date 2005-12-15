package nullnessAnnotations;

import edu.umd.cs.findbugs.annotations.*;

public class TestNonNull2 extends TestNonNull1 {

	Object f(Object o) {
		return o;
	}

	Object baz() {
		return f(null);
	}
}
