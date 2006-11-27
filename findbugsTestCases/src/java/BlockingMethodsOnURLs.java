import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * equals and hashCode are blocking methods on URLs
 * @author pugh
 *
 */
public class BlockingMethodsOnURLs {
	static int f(URL u) {
		return u.hashCode();
	}
	static boolean g(URL u1, URL u2) {
		return u1.equals(u2);
	}
	static Set<URL> foo() {
		return new HashSet<URL>();
	}
	static Map<URL,String> foo2() {
		return new HashMap<URL,String>();
	}
	static Map<String, URL> falsePositive() {
		return new HashMap<String, URL>();
	}
	public static Map<URL, String> map;
	public static Map<String, URL> falsePositiveMap;
	public static Set<URL> set;
}
