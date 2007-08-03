package jsr305;

import javax.annotation.meta.When;

public class TestViolatedInheritedAnnotations implements I1, I2 {
	@Foo(when=When.ALWAYS) Object always;
	@Foo(when=When.NEVER) Object never;

	public Object alwaysReturnFoo1() {
		return never;
	}

	public Object neverReturnFoo1() {
		return always;
	}

	public Object alwaysReturnFooParams1(Object alwaysParam, Object neverParam) {
		return neverParam;
	}
}
