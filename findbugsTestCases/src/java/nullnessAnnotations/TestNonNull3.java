package nullnessAnnotations;

import edu.umd.cs.findbugs.annotations.*;

@DefaultAnnotationForParameters(NonNull.class)
public class TestNonNull3 {

	Object f(Object o) {
		return o;
	}

	Object bar() {
		return f(null);
	}
}
