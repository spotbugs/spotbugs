package nullnessAnnotations;


public class TestNonNull2 extends TestNonNull1 {

	@Override
    Object f(Object o) {
		return o;
	}

	Object baz() {
		return f(null);
	}
}
