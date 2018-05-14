package ghIssues;

import java.util.Objects;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public enum Issue616 {
	TEST("one", "two", null);

	public final @NonNull String one;
	public final @NonNull String two;
	@Nullable
	public final String three;

	private Issue616(@NonNull String one, @NonNull String two, @Nullable Object three) {
		this.one = Objects.requireNonNull(one);
		this.two = Objects.requireNonNull(two);
		this.three = three == null ? null : three.toString();
	}
}
