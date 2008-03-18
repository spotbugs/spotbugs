package sfBugs;

import java.lang.reflect.Method;

public class Bug1912994 {
	public String foo() throws Exception {
		String s = null;
		try {
			Method m = this.getClass().getMethod("foo");
			m.invoke(this);
			char[] t = new char[0];
			t[1] = 'a';
			s = "foo";
			s += "bar";
			// Thread.sleep(100);
			return s;
		} catch (Exception e) { // Should yield "L D REC"
			throw new Exception(e);
		}
	}

	// If you comment out the following method, no "L D REC" warning is
	// emitted for line 17
	public String foo2() throws Exception {
		String s = null;
		try {
			Method m = this.getClass().getMethod("foo2");
			m.invoke(this);
			char[] t = new char[0];
			t[1] = 'a';
			s = "foo";
			s += "bar";
			// Thread.sleep(100);
			return s;
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new Exception(e);
		}
	}
}
