package jsr305;

import javax.annotation.meta.When;

@DefaultFooParameters
public class TestDefaultAnnotations {
	
	// parameter "o" must carry a @Foo(when=When.ALWAYS) type qualifier,
	// since that is the default for parameters
	public void requiresFoo(Object o) {
		
	}
	
	// violation: @Foo(when=When.NEVER) value passed to method expecting @Foo(when=When.ALWAYS)
	public void violate(@Foo(when=When.NEVER) Object x) {
		requiresFoo(x);
	}
	
	public void ok(Object x) {
		requiresFoo(x);
	}
	
	public void ok2(@Foo(when=When.ALWAYS) Object x) {
		requiresFoo(x);
	}

}
