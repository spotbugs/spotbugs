package sfBugs;

public class Bug1911620 {
	public long getLongMinus1(String longStr) {
		long l = Long.valueOf(longStr);
		return --l;
	}
}
