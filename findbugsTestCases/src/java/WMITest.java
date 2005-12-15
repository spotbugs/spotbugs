import java.util.*;

public class WMITest {
	public void test(Map m) {
		Iterator it = m.keySet().iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			String value = (String) m.get(name);
			System.out.println(name + " = " + value);
		}
	}

	public void test2(int a, int b, int c, int d, Map m) {
		Iterator it = m.keySet().iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			String value = (String) m.get(name);
			System.out.println(name + " = " + value);
		}
	}

	public void test3(Map m) {
		Set s = m.keySet();
		Iterator it = s.iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			String value = (String) m.get(name);
			System.out.println(name + " = " + value);
		}
	}

	public void test4(Map m) {
		Iterator it = m.keySet().iterator();
		while (it.hasNext()) {
			Object name = it.next();
			String value = (String) m.get(name);
			System.out.println(name.toString() + " = " + value);
		}
	}

}