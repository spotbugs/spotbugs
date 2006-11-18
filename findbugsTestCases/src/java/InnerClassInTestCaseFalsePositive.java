import junit.framework.TestCase;


/**
 IJU: TestCase AccounterTest$1 has no tests
I got this one for bringing mock objects in my test code like this:
public void testFoo() {
	doSomething(new FooBar() {
		public void overrideSomeMethod() {
			// ...
		}
	});
	// ...
}
Note that FooBar is not even derived from TestCase.

 *
 */
public class InnerClassInTestCaseFalsePositive extends TestCase {
	public void testFoo() {
		doSomething(new Object() {
			public void overrideSomeMethod() {
				// ...
			}
		});
		// ...
	}

	private void doSomething(Object object) {
		// TODO Auto-generated method stub
		
	}
}
