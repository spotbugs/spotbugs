package nullnessAnnotations.packageDefault;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

class TestNonNull2 extends TestNonNull1 implements Interface1 {
	
	@ExpectWarning("NP")
	void report1() {
		f(null); // should get a NonNull warning from TestNonNull1
	}
	
//	@ExpectWarning("NP")
	void report2() {
		//
		// FindBugs doesn't produce a warning here because the g()
		// method in TestNonNull1 explicitly marks its parameter
		// as @Nullable.  So, we shouldn't expect a warning. (?)
		//
		g(null); // should get a NonNull warning from Interface1
	}
	
	@NoWarning("NP")
	void ok1() {
		h(null); // should be OK
	}
}
