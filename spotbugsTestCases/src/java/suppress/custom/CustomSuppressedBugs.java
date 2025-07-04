package suppress.custom;

public class CustomSuppressedBugs {
	@SuppressFBWarnings(value = "NP")
	public int suppressedNpePrefix() {
		// NP_ALWAYS_NULL and NP_LOAD_OF_KNOWN_NULL_VALUE
		String x = null;
		return x.length();
	}
	
	@SuppressFBWarnings(value = {"NP_ALWAYS_NULL", "NP_LOAD_OF_KNOWN_NULL_VALUE"})
	public int suppressedNpeExact() {
		// NP_ALWAYS_NULL and NP_LOAD_OF_KNOWN_NULL_VALUE
		String x = null;
		return x.length();
	}
}
