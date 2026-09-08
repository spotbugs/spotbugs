package ghIssues;

import java.util.Objects;
import java.util.function.Function;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class Issue1778 {
	@CheckForNull
	public static Function<String, String> get() {
		return k -> "nope";
	}
	
	public static String getMapping(String value) {
		Objects.requireNonNull(value);
		
		Function<String, String> f = get();
		Objects.requireNonNull(f);
		
		return f.apply(value);
	}
}
