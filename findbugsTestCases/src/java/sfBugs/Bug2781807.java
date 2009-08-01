package sfBugs;

public class Bug2781807 {
	private Object field;

	public void method() {
		if (field instanceof String) {
			String fieldText = (String) field;
			field = Integer.valueOf(fieldText);
		}
	}
}