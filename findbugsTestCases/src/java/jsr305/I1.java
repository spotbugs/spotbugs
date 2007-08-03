package jsr305;

import javax.annotation.meta.When;

public interface I1 {
	public @Foo(when=When.ALWAYS) Object alwaysReturnFoo1();
	
	public @Foo(when=When.NEVER) Object neverReturnFoo1();
	
	public @Foo(when=When.ALWAYS) Object alwaysReturnFooParams1(
			@Foo(when=When.ALWAYS) Object alwaysParam,
			@Foo(when=When.NEVER) Object neverParam);
}
