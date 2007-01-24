package npe;

import java.util.HashMap;
import java.util.Map;

class NP {

	int x;

	static Map m = new HashMap();

	public static void main(String args[]) {
		NP n = (NP) m.get("Foo");

		if (n == null)
			System.out.println(n.x);
		else
			System.out.println(n.x);
		if (n != null)
			System.out.println(n.x);
		else
			System.out.println(n.x);

		System.out.println(null != n ? n.hashCode() : 0);
	}
}
