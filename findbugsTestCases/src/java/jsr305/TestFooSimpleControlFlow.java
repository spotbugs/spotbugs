package jsr305;

import javax.annotation.meta.When;



public class TestFooSimpleControlFlow {
	
	public @Foo(when=When.NEVER) Object notFoo;
	public @Foo(when=When.ALWAYS) Object foo;
	
	public @Foo(when=When.NEVER) Object returnsNotFoo() { return null; };
	public @Foo(when=When.ALWAYS) Object returnsFoo() { return null; };
	
	public void requiresNotFoo( @Foo(when=When.NEVER)  Object x) {  };
	public void requiresFoo( @Foo(when=When.ALWAYS)  Object x) {  };
	boolean b;
	public Object unknown;
	
	public void test12() {
		Object notFoo2 = notFoo;
		if (b) notFoo2 = unknown;
		foo = notFoo2;
	}
	public void test32() {
		Object returnsNotFoo = returnsNotFoo();
		if (b) returnsNotFoo = unknown;
		foo = returnsNotFoo;
	}
	
	public void test21() {
		Object foo2 = foo;
		if (b) foo2 = unknown;
		notFoo = foo2;
	}
	public void test41() {
		Object returnsFoo = returnsFoo();
		if (b) returnsFoo = unknown;
		notFoo = returnsFoo;
	}
	
	public void test16() {
		Object notFoo2 = notFoo;
		if (b) notFoo2 = unknown;
		requiresFoo(notFoo2);
	}
	public void test36() {
		Object returnsNotFoo = returnsNotFoo();
		if (b) returnsNotFoo = unknown;
		requiresFoo(returnsNotFoo);
	}
	public void test25() {
		Object foo2 = foo;
		if (b) foo2 = unknown;
		requiresNotFoo(foo2);
	}
	public void test45() {
		Object returnsFoo = returnsFoo();
		if (b) returnsFoo = unknown;
		requiresNotFoo(returnsFoo);
	}
	
	
	public void ok22() {
		Object foo2 = foo;
		if (b) foo2 = unknown;
		foo = foo2;
	}
	public void ok42() {
		Object returnsFoo = returnsFoo();
		if (b) returnsFoo = unknown;
		foo = returnsFoo;
	}
	
	public void ok11() {
		Object notFoo2 = notFoo;
		if (b) notFoo2 = unknown;
		notFoo = notFoo2;
	}
	public void ok31() {
		Object returnsNotFoo = returnsNotFoo();
		if (b) returnsNotFoo = unknown;
		notFoo = returnsNotFoo;
	}
	
	public void ok26() {
		Object foo2 = foo;
		if (b) foo2 = unknown;
		requiresFoo(foo2);
	}
	public void ok46() {
		Object returnsFoo = returnsFoo();
		if (b) returnsFoo = unknown;
		requiresFoo(returnsFoo);
	}
	public void ok15() {
		Object notFoo2 = notFoo;
		if (b) notFoo2 = unknown;
		requiresNotFoo(notFoo2);
	}
	public void ok35() {
		Object returnsNotFoo = returnsNotFoo();
		if (b) returnsNotFoo = unknown;
		requiresNotFoo(returnsNotFoo);
	}
	
	

}
