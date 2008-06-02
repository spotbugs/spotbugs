package jsr305;

/**
 * See if FindBugs does the right thing when
 * a call to a method
 */
public class TrickyInheritedQualifiers {
	static class X {
		public void f(@Strict Object p) {
			
		}
	}
	
	static class Y extends X {
		//
		// NOTE: @Strict annotation on parameter p should be
		//       inherited from overridden superclass method
		//
		public void f(Object p) {
			
		}
	}
	
	public void violateEasy(X x, Object q) {
		//
		// An easy-to-check violation: X.f() is directly
		// annotated with @Strict, and it is passed
		// a non-@Strict (when=UNKNOWN) value.
		//
		x.f(q);
	}
	
	public void violateTricky(Y y, Object q) {
		//
		// This is a violation:
		// value q, not annotated with the @Strict annotation
		// (and thus effectively when=UNKNOWN)
		// is passed to a method requiring a @Strict (when=ALWAYS) value.
		//
		y.f(q);
	}
}
