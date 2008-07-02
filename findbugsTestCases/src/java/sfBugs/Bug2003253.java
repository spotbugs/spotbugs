package sfBugs;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Bug2003253 {
	@Nonnull
	private Object foo;

	//
	// The InconsistentAnnotations detector should report
	// a warning here.
	//
	@ExpectWarning("NP")
	public void report1(@Nullable Object bar) {
		this.foo = bar;
	}

	//
	// FindNullDeref should report a warning here.
	//
	@ExpectWarning("NP")
	public void report2(@CheckForNull Object bar) {
		this.foo = bar;
	}
}
