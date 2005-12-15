package nullnessAnnotations;

import edu.umd.cs.findbugs.annotations.NonNull;

class Test6 {

	void f(double x, @NonNull
	Object a, Object b, @NonNull
	Object c) {
	}

	void g(@NonNull
	Object a, Object b, @NonNull
	Object c, double x) {
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
}
