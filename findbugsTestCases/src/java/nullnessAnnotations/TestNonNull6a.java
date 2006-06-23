package nullnessAnnotations;

import edu.umd.cs.findbugs.annotations.NonNull;

public class TestNonNull6a {
	public static int f(@NonNull Object o) {
		return o.hashCode();
	}

}
