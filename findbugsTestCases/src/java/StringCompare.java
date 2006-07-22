public class StringCompare {
	String x, y, z;

	public static boolean compare(StringCompare a, StringCompare b) {
		if (a == null & b == null)
			return false;
		if (a == null ^ b == null)
			return true;
		return a.x.equals(b.x) & a.y.equals(b.y) & a.z.equals(b.z);
	}

	public static boolean compare2(StringCompare a, StringCompare b) {
		return a.x.equals(b.x) & a.y.equals(b.y);
	}

}
