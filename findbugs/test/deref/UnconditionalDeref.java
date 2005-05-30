package deref;

public class UnconditionalDeref {
	
	void f(Object obj) {
		System.out.println(obj.hashCode());
	}
	
	void report() {
		f(null);
	}
	
	Object returnsNull() {
		return null;
	}
	
	void report2() {
		Object o = returnsNull();
		System.out.println(o.hashCode());
	}
}
