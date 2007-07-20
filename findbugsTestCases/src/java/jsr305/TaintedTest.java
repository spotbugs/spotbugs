package jsr305;

import javax.annotation.Tainted;
import javax.annotation.Untainted;

public abstract class TaintedTest {
	@Untainted Object sanitize(Object o) {
		return o;
	}
	
	void correctDoNotReport(@Tainted Object b) {
		Object x = sanitize(b);
		requiresUntainted(x);
	}
	
	void violationReport(@Tainted Object a) {
		Object y = a;
		requiresUntainted(y);
	}
	
	protected abstract void requiresUntainted(@Untainted Object o);
}
