
public class UselessControlFlow {

	void harmless1(Object x, Object y) {
		if (!x.equals(y)) {

		} else {

		}
		System.out.println(x);
		System.out.println(y);
	}
	void report0(Object x, Object y) {
		if (!x.equals(y)); System.out.println(x);
		System.out.println(y);
	}
	void report1(Object x, Object y) {
		if (!x.equals(y));
		  System.out.println(x);
		System.out.println(y);
	}
	void report2(Object x, Object y) {
		if (!x.equals(y));

		  System.out.println(x);
		System.out.println(y);
	}
	void report3(Object x, Object y) {
		if (!x.equals(y));


		  System.out.println(x);
		System.out.println(y);
	}
	void report4(Object [] x, Object y) {
		for(Object o : x) {
			if (o.equals(y)) {
			}
		}
	}
}
