package ghIssues;

public class Issue1928 {

	public String builder() {
		StringBuilder builder = new StringBuilder();
		builder.append("x");
		
		return builder.substring(0);
	}

	public String buffer() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("x");
		
		return buffer.substring(0);
	}

	public String simpleString() {
		String value = "x";
		
		return value.substring(0);
	}
}
