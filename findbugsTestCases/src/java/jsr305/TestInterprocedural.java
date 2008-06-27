package jsr305;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public abstract class TestInterprocedural {
	@AlwaysBlue Object blueField;
	
	protected abstract void requiresAlwaysBlue(@AlwaysBlue Object obj);
	@NeverBlue protected abstract Object f();

	// Requires a value that is always blue,
	// but is not annotated as such.
	@NoWarning("TQ")
	protected void requiresAlwaysBlueButNotAnnotatedAsSuch(Object o) {
		requiresAlwaysBlue(o);
	}

	// Returns a value that is never blue,
	// but does not have any direct annotations.
	@NoWarning("TQ")
	public Object g() {
		return f();
	}
	
	@ExpectWarning("TQ")
	public void report1() {
		Object neverBlue = g();
		blueField = neverBlue;
	}
	
	@ExpectWarning("TQ")
	public void report2() {
		Object neverBlue = f();
		requiresAlwaysBlueButNotAnnotatedAsSuch(neverBlue);
	}
}
