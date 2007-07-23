package jsr305;

import javax.annotation.meta.When;



public class TestFooNoControlFlow2 {
	
	public @Foo(when=When.NEVER) Object notFoo;
	public @Foo(when=When.ALWAYS) Object foo;
	
	public @Foo(when=When.NEVER) Object returnsNotFoo() { return null; };
	public @Foo(when=When.ALWAYS) Object returnsFoo() { return null; };
	
	public void requiresNotFoo( @Foo(when=When.NEVER)  Object x) {  };
	public void requiresFoo( @Foo(when=When.ALWAYS)  Object x) {  };
	
	
	public void test12() {
		Object notFoo2 = notFoo;
		foo = notFoo2;
	}
	public void test32() {
		Object returnsNotFoo = returnsNotFoo();
		foo = returnsNotFoo;
	}
	
	public void test21() {
		Object foo2 = foo;
		notFoo = foo2;
	}
	public void test41() {
		Object returnsFoo = returnsFoo();
		notFoo = returnsFoo;
	}
	
	public void test16() {
		Object notFoo2 = notFoo;
		requiresFoo(notFoo2);
	}
	public void test36() {
		Object returnsNotFoo = returnsNotFoo();
		requiresFoo(returnsNotFoo);
	}
	public void test25() {
		Object foo2 = foo;
		requiresNotFoo(foo2);
	}
	public void test45() {
		Object returnsFoo = returnsFoo();
		requiresNotFoo(returnsFoo);
	}
	
	
	public void ok22() {
		Object foo2 = foo;
		foo = foo2;
	}
	public void ok42() {
		Object returnsFoo = returnsFoo();
		foo = returnsFoo;
	}
	
	public void ok11() {
		Object notFoo2 = notFoo;
		notFoo = notFoo2;
	}
	public void ok31() {
		Object returnsNotFoo = returnsNotFoo();
		notFoo = returnsNotFoo;
	}
	
	public void ok26() {
		Object foo2 = foo;
		requiresFoo(foo2);
	}
	public void ok46() {
		Object returnsFoo = returnsFoo();
		requiresFoo(returnsFoo);
	}
	public void ok15() {
		Object notFoo2 = notFoo;
		requiresNotFoo(notFoo2);
	}
	public void ok35() {
		Object returnsNotFoo = returnsNotFoo();
		requiresNotFoo(returnsNotFoo);
	}
	
	

}
