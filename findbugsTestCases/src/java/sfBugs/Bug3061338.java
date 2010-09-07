package sfBugs;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NonNull;

public class Bug3061338 {

	@DesireWarning("NP_NONNULL_PARAM_VIOLATION")
	public void doesntWork() {
		TestClassTwo two = null;
		TestClassOne one = new TestClassOne();

		for (int i = 0; i < 5; i++) {
			// two is null and should be identified it as an invalid null input.
			one.getTwo(two);
		}
	}
	@ExpectWarning("NP_NONNULL_PARAM_VIOLATION")
	public void alreadyWorks() {
		TestClassTwo two = null;
		TestClassOne one = new TestClassOne();

		for (int i = 0; i < 5; i++) {
			two = null;
			one.getTwo(two);
		}
	}


	public static class TestClassOne {

		public TestClassTwo getTwo(@NonNull TestClassTwo two) {
			return two;
		}
	}

	public static class TestClassTwo {
	}
}
