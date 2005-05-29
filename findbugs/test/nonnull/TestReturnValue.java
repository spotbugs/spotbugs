package nonnull;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.PossiblyNull;

abstract class Foo {
	protected abstract @PossiblyNull Object getPossiblyNull();
	public abstract @NonNull Object reportReturnNull();
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

	// Override a method with a @NonNull return annotation
	public Object reportNonNull() {
		return null;
	}
	
	// Even more blatant: violate a annotation directly
	public @NonNull Object reportNonNull2() {
		return null;
	}
	
	// This is fine
	public @PossiblyNull Object doNotReportReturnPossiblyNull() {
		return null;
	}
}
