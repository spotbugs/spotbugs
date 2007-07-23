package jsr305;

import javax.annotation.meta.When;



public class TestFooNoControlFlow {
	
	public @Foo(when=When.NEVER) Object notFoo;
	public @Foo(when=When.ALWAYS) Object foo;
	
	public @Foo(when=When.NEVER) Object returnsNotFoo() { return null; };
	public @Foo(when=When.ALWAYS) Object returnsFoo() { return null; };
	
	public void requiresNotFoo( @Foo(when=When.NEVER)  Object x) {  };
	public void requiresFoo( @Foo(when=When.ALWAYS)  Object x) {  };
	
	
	public void test12() {
		foo = notFoo;
	}
	public void test32() {
		foo = returnsNotFoo();
	}
	
	public void test21() {
		notFoo = foo;
	}
	public void test41() {
		notFoo = returnsFoo();
	}
	
	public void test16() {
		requiresFoo(notFoo);
	}
	public void test36() {
		requiresFoo(returnsNotFoo());
	}
	public void test25() {
		requiresNotFoo(foo);
	}
	public void test45() {
		requiresNotFoo(returnsFoo());
	}
	
	
	public void ok22() {
		foo = foo;
	}
	public void ok42() {
		foo = returnsFoo();
	}
	
	public void ok11() {
		notFoo = notFoo;
	}
	public void ok31() {
		notFoo = returnsNotFoo();
	}
	
	public void ok26() {
		requiresFoo(foo);
	}
	public void ok46() {
		requiresFoo(returnsFoo());
	}
	public void ok15() {
		requiresNotFoo(notFoo);
	}
	public void ok35() {
		requiresNotFoo(returnsNotFoo());
	}
	
	

}
