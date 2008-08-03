
public class TestFormatString {

	
	public static String ok(String tagName) {
		return String.format("</%1$s> tag with no <%1$s>", tagName);
	}
}
