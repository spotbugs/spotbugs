package nonnull;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.CheckForNull;

abstract class Foo {
	protected abstract @CheckForNull
	Object getCheckForNull();

	public abstract @NonNull
	Object reportReturnNull();
}

abstract class Bar extends Foo {
}

public abstract class TestReturnValue {
	public void report(Foo f) {
		Object obj = f.getCheckForNull();
		System.out.println(obj.hashCode());
	}

	public void reportCallThroughSubclass(Bar b) {
		Object obj = b.getCheckForNull();
		System.out.println(obj.hashCode());
	}

	// Override a method with a @NonNull return annotation
	public Object reportNonNull() {
		return null;
	}

	// Even more blatant: violate a annotation directly
	public @NonNull
	Object reportNonNull2() {
		return null;
	}

	// This is fine
	public @CheckForNull
	Object doNotReportReturnCheckForNull() {
		return null;
	}
}
