package npe;

import edu.umd.cs.findbugs.annotations.NonNull;

public class SinksRequiringNonNull {

	void f(@NonNull Object x) {}

    int g(Object x) { return x.hashCode(); }

	@NonNull Object f;

    void testDirectDereference(Object x) {
		x.hashCode();
	}
	void testPassedToParameterAnnotationNonnull(Object x) {
        f(x);
	}
	void testPassedToParameterThatIsAlwaysDereferenced(Object x) {
		f(x);
    }
	@NonNull Object testReturnedFromMethodRequiringNonNull(Object x) {
		return x;
	}
    void testAssignedToNonNullField(Object x) {
		this.f = x;
	}
}
