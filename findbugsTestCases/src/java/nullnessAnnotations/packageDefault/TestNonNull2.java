package nullnessAnnotations.packageDefault;

class TestNonNull2 extends TestNonNull1 implements Interface1 {

	void baz() {

		f(null); // should get a NonNull warning from TestNonNull1
		g(null); // should get a NonNull warning from Interface1
		h(null); // should be OK
	}
}
