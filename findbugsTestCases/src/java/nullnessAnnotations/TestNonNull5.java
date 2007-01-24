package nullnessAnnotations;

import edu.umd.cs.findbugs.annotations.NonNull;

public class TestNonNull5 {

	@NonNull
	Object f() {
		return null;
	}

}
