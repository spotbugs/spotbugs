package infiniteLoop;

import java.util.List;

public class InfiniteLoop {
	
	void report() {
		report();
	}
	
	void report2(Object a, Object b) {
		if (a.equals(b))
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
	
	void report4(List<Object> list) {
		list.add(list);
	}
}
