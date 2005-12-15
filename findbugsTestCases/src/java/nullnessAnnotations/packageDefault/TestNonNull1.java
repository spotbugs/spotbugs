package nullnessAnnotations.packageDefault;

import edu.umd.cs.findbugs.annotations.*;

public class TestNonNull1 {

	public Object f(Object o) {
		return o;
	}

	public Object g(@Nullable
	Object o) {
		return o;
	}

	public Object h(@Nullable
	Object o) {
		return o;
	}

	Object bar() {
		return f(null);
	}
}
