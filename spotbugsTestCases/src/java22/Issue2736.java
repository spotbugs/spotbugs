import java.util.List;

public class Issue2736 {
	public void catchBlock(String s) {
		try {
			int i = Integer.parseInt(s);
		} catch (NumberFormatException _) { // Unnamed variable
			System.out.println("Bad number: " + s);
		}
	}

	public int typeSwitch(List<String> values) {
		int i = 0;
		for (String _ : values) { // Unnamed variable
			i++;
		}
		return i;
	}

	public int variable(List<String> values) {
		var _ = values.remove(0); // Unnamed variable
		var _ = values.remove(0); // Unnamed variable
		var x = values.remove(0);

		return x.length();
	}

	public int typeSwitch(Object x) {
		return switch (x) {
			case String _ -> 1; // Unnamed variable
			case Integer _ -> 2; // Unnamed variable
			case Long abc -> 3;
			default -> 42;
		};
	}
}
