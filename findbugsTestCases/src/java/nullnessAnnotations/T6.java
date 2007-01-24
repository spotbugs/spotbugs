package nullnessAnnotations;

import edu.umd.cs.findbugs.annotations.NonNull;

public class T6 {

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
		}
}
