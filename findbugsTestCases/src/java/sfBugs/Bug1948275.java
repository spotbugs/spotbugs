package sfBugs;

public class Bug1948275 {
	public static String trim(String value) {
		return value == null ? value : value.trim(); // should not report
	}
	
	public static String trim2(String value) {
		if(value == null) {
			return value; // should not report
		}
		return "Hello";
	}

	public static String trim3(String value) {
		if(value == null) {
			String s = "Hello";
			s += value; // should report
			return s;
		}
		return "Hello";
	}
}
