package nullnessAnnotations.packageDefault;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

class TestNonNull2 extends TestNonNull1 implements Interface1 {
	
	@ExpectWarning("NP")
	void report1() {
		f(null); // should get a NonNull warning from TestNonNull1
	}
	
	@ExpectWarning("NP")
	void report2() {
		g(null); // should get a NonNull warning from Interface1
	}
	
	@NoWarning("NP")
	void ok1() {
		h(null); // should be OK
	}
}
