package npe;

import java.util.*;

class NullDeref6 {

	// from code in commons-collection,
	// org.apache.commons.collections.IteratorUtils.getIterator

	Object foo(Object o) {
		if (o == null)
			return o;

		if (o instanceof String)
			return o;

		// we should flag the o != null test as redundant
		if (o != null && o instanceof Set)
			return ((Set) o).iterator();

		// no warning should be generated here
		return o.getClass();
	}

	Object bar(Object o) {
		if (o != null)
			return o;

		if (o == null)
			System.out.println("Got null");

		// Should get high priority
		System.out.println(o.hashCode());

		if (o == null)
			return o;
		// Unreachable code
		return o.getClass();
	}

}
