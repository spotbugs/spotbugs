package suppress;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.annotations.SuppressMatchType;

public class SuppressedBugs {

	@SuppressFBWarnings(value = "NP")
	public int suppressedNpePrefix() {
		// NP_ALWAYS_NULL and NP_LOAD_OF_KNOWN_NULL_VALUE
		String x = null;
		return x.length();
	}

	@SuppressFBWarnings(value = "XYZ")
	public int nonSuppressedNpePrefix() {
		// NP_ALWAYS_NULL and NP_LOAD_OF_KNOWN_NULL_VALUE
		String x = null;
		return x.length();
	}

	@SuppressFBWarnings(value = {"NP_ALWAYS_NULL", "NP_LOAD_OF_KNOWN_NULL_VALUE"}, matchType = SuppressMatchType.EXACT)
	public int suppressedNpeExact() {
		// NP_ALWAYS_NULL and NP_LOAD_OF_KNOWN_NULL_VALUE
		String x = null;
		return x.length();
	}

	@SuppressFBWarnings(value = "XYZ", matchType = SuppressMatchType.EXACT)
	public int nonSuppressedNpeExact() {
		// NP_ALWAYS_NULL and NP_LOAD_OF_KNOWN_NULL_VALUE
		String x = null;
		return x.length();
	}

	@SuppressFBWarnings(value = "NP.*", matchType = SuppressMatchType.REGEX)
	public int suppressedNpeRegex() {
		// NP_ALWAYS_NULL and NP_LOAD_OF_KNOWN_NULL_VALUE
		String x = null;
		return x.length();
	}

	@SuppressFBWarnings(value = "XYZ.*", matchType = SuppressMatchType.REGEX)
	public int nonSuppressedNpeRegex() {
		// NP_ALWAYS_NULL and NP_LOAD_OF_KNOWN_NULL_VALUE
		String x = null;
		return x.length();
	}
}
