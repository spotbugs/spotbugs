package nullnessAnnotations;

import edu.umd.cs.findbugs.annotations.*;

public class TestNonNull5 {

	@NonNull
	Object f() {
		return null;
	}

}
