package npe;

import junit.framework.Assert;

public class JunitFailFalsePositive extends Assert {
	int f(Object x) {
		if (x == null)
			fail("x is null");
		return x.hashCode();
	}

}
