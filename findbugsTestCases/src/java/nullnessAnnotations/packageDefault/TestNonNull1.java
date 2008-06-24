package nullnessAnnotations.packageDefault;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.Nullable;

public class TestNonNull1 {

	public Object f(Object o) {
		return o;
	}

	public Object g(@Nullable Object o) {
		return o;
	}

	public Object h(@Nullable Object o) {
		return o;
	}

	@ExpectWarning("NP")
	public Object bar() {
		return f(null); // warning: f()'s parameter is non-null
	}
}
