import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public record Issue3622(int x) {
	@Override
	@SuppressFBWarnings
	public final boolean equals(Object x) {
		return true;
	}
}