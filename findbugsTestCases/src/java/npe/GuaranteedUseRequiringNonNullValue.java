package npe;

import edu.umd.cs.findbugs.annotations.NonNull;

public class GuaranteedUseRequiringNonNullValue {

	static int f(@NonNull Object x) {
		return 17;
    }

	static int ff( Object x) {
		return x.hashCode();
    }

	int gNull() {
		Object x = null;
        return f(x);
	}
	int gNSP(Object x) {
		if (x == null) {
            System.out.println("x is null");
		}
		return f(x);
	}
    int gNCP(Object x, boolean b) {
		if (x == null) {
			System.out.println("x is null");
		}
        if (b)
			System.out.println("b is true");
		return f(x);
	}
    int gg(Object x, boolean b) {
		if (x == null) {
			System.out.println("x is null");
		}
        if (b)
			return ff(x);
		return f(x);
	}
    
	@NonNull Object h(Object x) {
		if (x == null) {
			System.out.println("x is null");
        }
		return x;
	}

    @NonNull   Object h(Object x, boolean b) {
		if (x == null) {
			System.out.println("x is null");
		}
        if (b)
			System.out.println("b is true");
		return x;
	}
    


}
