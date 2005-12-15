
public class CreateStaticObject {
	public void test() {
		Utils u = new Utils();

		int count = u.getMaxTimes();

		while (count-- > 0) {
			System.out.println(u.getGreeting());
		}
	}
}

class Utils {
	public static String getGreeting() {
		return "Hello";
	}

	public static int getMaxTimes() {
		return 3;
	}
}

class TypeSafeEnum {
	public static TypeSafeEnum ONE = new TypeSafeEnum();

	public static TypeSafeEnum TWO = new TypeSafeEnum();

	private TypeSafeEnum() {
	}
}