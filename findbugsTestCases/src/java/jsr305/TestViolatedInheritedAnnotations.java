package jsr305;

import javax.annotation.Tainted;
import javax.annotation.meta.When;

import jsr305.package1.InterfaceWithDefaultUntaintedParams;

public class TestViolatedInheritedAnnotations implements I1, I2 {
	@Foo(when=When.ALWAYS) Object always;
	@Foo(when=When.NEVER) Object never;
	
	@Bar(when=When.MAYBE, strArrField={"yip", "yip"}, cField='Q') Object barField;

	public Object alwaysReturnFoo1() {
		return never;
	}

	public Object neverReturnFoo1() {
		return always;
	}

	// This method inherits parameter and return value annotations from I1
	public Object alwaysReturnFooParams1(Object alwaysParam, Object neverParam) {
		return neverParam;
	}
	
	public void needsUntaintedParam(@Tainted Object tainted, InterfaceWithDefaultUntaintedParams obj) {
		// Should see a warning here
		obj.requiresUntaintedParam(tainted);
	}
}
