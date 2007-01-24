package nullnessAnnotations;

import edu.umd.cs.findbugs.annotations.NonNull;

class Test6 {

	void f(double x, @NonNull
	Object a, Object b, @NonNull
	Object c) {
	}

	void g(@NonNull Object a, Object b, @NonNull Object c, double x) {
	}

	int f2(double x, Object a, Object b, Object c) {
		return a.hashCode() + c.hashCode();
	}

	int g2(Object a, Object b, Object c, double x) {
		return a.hashCode() + c.hashCode();
	}

	void bar() {
		// Good calls
		f(1.0, this, null, this);
		g(this, null, this, 1.0);
		f2(1.0, this, null, this);
		g2(this, null, this, 1.0);
		// bad calls
		f(1.0, null, this, this);
		g(null, this, this, 1.0);
		f2(1.0, null, this, this);
		g2(null, this, this, 1.0);
		f(1.0, this, this, null);
		g(this, this, null, 1.0);
		f2(1.0, this, this, null); // thinks this is OK?
		g2(this, this, null, 1.0);
	}
	
	void bar2(Object n) {
		if (n == null) 
			System.out.println("n is null");
		
		// Good calls
		f(1.0, this, n, this);
		g(this, n, this, 1.0);
		f2(1.0, this, n, this);
		g2(this, n, this, 1.0);
		// bad calls
		f(1.0, n, this, this);
		g(n, this, this, 1.0);
		f2(1.0, n, this, this);
		g2(n, this, this, 1.0);
		f(1.0, this, this, n);
		g(this, this, n, 1.0);
		f2(1.0, this, this, n); // thinks this is OK?
		g2(this, this, n, 1.0);
	}
	void bar3(Object n, boolean b) {
		if (n == null) 
			System.out.println("n is null");
		if (b)
			System.out.println("b is true");
		// Good calls
		f(1.0, this, n, this);
		g(this, n, this, 1.0);
		f2(1.0, this, n, this);
		g2(this, n, this, 1.0);
		// bad calls
		f(1.0, n, this, this);
		g(n, this, this, 1.0);
		f2(1.0, n, this, this);
		g2(n, this, this, 1.0);
		f(1.0, this, this, n);
		g(this, this, n, 1.0);
		f2(1.0, this, this, n); // thinks this is OK?
		g2(this, this, n, 1.0);
	}
}
