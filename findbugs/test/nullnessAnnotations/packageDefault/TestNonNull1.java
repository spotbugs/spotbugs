package nullnessAnnotations.packageAnnotation;

import edu.umd.cs.findbugs.annotations.*;

public class TestNonNull1 {

  Object f(Object o) {
	return o;
	}

  Object bar() {
	return f(null);
	}
}
