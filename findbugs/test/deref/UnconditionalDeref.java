package deref;

public class UnconditionalDeref {
	
	void f(Object obj) {
		System.out.println(obj.hashCode());
	}
	
	void report() {
		f(null);
	}
}
