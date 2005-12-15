package infiniteLoop;

import java.util.List;

public abstract class InfiniteLoop {

	int x;

	int y;

	void report() {
		report();
	}

	void report2(Object a, Object b) {
		if (a.equals(b)) // we miss this one because we assume equals can do
							// a store
			report2(a, b);
	}

	static void report3(InfiniteLoop obj) {
		obj.report3(obj);
	}

	void doNotReport(Object a, Object b) {
		if (a.equals(b)) {
			doNotReport(b, a);
		}
	}

	void doNotReport2(Object a, Object b) {
		if (x == 0) {
			x = 1;
			// A field has been checked and modified
			doNotReport2(a, b);
		}
	}

	void doNotReport3(Object a, Object b) {
		if (opaque()) {
			// Assume method invocation reads and writes all fields
			doNotReport3(a, b);
		}
	}

	void report4(Object a, Object b) {
		if (x == 0) {
			y = 1;
			// no field has been both read and written!
			report4(a, b);
		}
	}

	void report5(List<Object> list) {
		list.add(list);
	}

	protected abstract boolean opaque();
}
