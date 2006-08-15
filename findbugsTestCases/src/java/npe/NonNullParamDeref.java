package npe;

import edu.umd.cs.findbugs.annotations.NonNull;

public class NonNullParamDeref {
	static void foo(@NonNull Object o) {
		
	}
	
	void bar(@NonNull Object o) {
		
	}
	
	int count;
	
	void f(boolean a) {
		
		Object x = null;
		if (a) {
			x = new Object();
		}
		
		foo(x);
	}
	
	void g(boolean b) {
		Object x = null;
		
		if (b) {
			x = new Object();
		}
		
		bar(x);
	}
	
}
