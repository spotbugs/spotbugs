package sfBugs;

public class Bug1911620 {
	public long getLongMinus1(String longStr) {
		long l = Long.valueOf(longStr);
		return --l;
	}

	public long getLongPlus1(String longStr) {
		long l = Long.valueOf(longStr);
		return ++l;
	}

	public long getLongWithDLS(String longStr) {
		long l = Long.valueOf(longStr);
		long l2 = l; // This is the only place FindBugs should give a DLS warning
		return l;  
	}

	public long getLongMinus1_2(String longStr) {
		long l = Long.valueOf(longStr);
		--l;
		return l;
	}

	public long getLongMinus2(String longStr) {
		long l = Long.valueOf(longStr);
		return l - 2;
	}

	public int getIntMinus1(String intStr) {
		int i = Integer.valueOf(intStr);
		return --i;
	}
}
