package sfBugs;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class Bug1633245 {
	interface Foo {
		int f(@CheckForNull Object x);
	}
    
	static class FooImpl implements Foo {
		public int f(Object x) {
			return x.hashCode();
        }
	}

}
