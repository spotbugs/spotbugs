package nonnull;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.PossiblyNull;

abstract class Foo {
	protected abstract @PossiblyNull Object getPossiblyNull();
}

abstract class Bar extends Foo {
}

public abstract class TestReturnValue {
	public void report(Foo f) {
		Object obj = f.getPossiblyNull();
		System.out.println(obj.hashCode());
	}
	
	public void reportCallThroughSubclass(Bar b) {
		Object obj = b.getPossiblyNull();
		System.out.println(obj.hashCode());
	}
}
