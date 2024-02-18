package ghIssues;

import java.util.function.Predicate;

public class Issue2837 {
	public boolean overridable(String s) {
		return true;
	}

	public final Predicate<String> predicate1 = this::overridable;
}