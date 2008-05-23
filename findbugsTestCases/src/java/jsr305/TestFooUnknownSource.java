package jsr305;

import javax.annotation.meta.When;

public class TestFooUnknownSource {

	int f(@NeverFoo String c) {
		return c.hashCode();
	}

	int g(@Foo String c) {
		return c.hashCode();
	}

	int unannotatedSourceToNeverSinkFalsePositive(String c) {
		return f(c); // should not generate a warning here
	}

	int unannotatedSourceToAlwaysSinkFalsePositive(String c) {
		return g(c); // should not generate a warning here
	}

	int unknownSourceToNeverSinkFalsePositive(@Foo(when = When.UNKNOWN) String c) {
		return f(c); // should generate a warning here
	}

	int unknownSourceToNeverSourceFalsePositive(@Foo(when = When.UNKNOWN) String c) {
		return g(c); // should generate a warning here
	}
}
