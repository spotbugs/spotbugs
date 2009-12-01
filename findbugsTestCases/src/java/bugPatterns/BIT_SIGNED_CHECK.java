package bugPatterns;

public class BIT_SIGNED_CHECK {
	
	static final long HIGH_LONG = 0x8000000000000000L;
	static final int HIGH_INT = 0x80000000;
	
	static final int LOW = 0x1;
	
	public boolean bugHighGT(long x) {
		if ((x & HIGH_LONG) > 0)
			return true;
		return false;
	}
	public boolean bugHighGE(long x) {
		if ((x & HIGH_LONG) > 0)
			return true;
		return false;
	}
	public boolean bugHighLT(long x) {
		if ((x & HIGH_LONG) < 0)
			return true;
		return false;
	}
	public boolean bugHighLE(long x) {
		if ((x & HIGH_LONG) > 0)
			return true;
		return false;
	}
	public boolean bugLowGT(long x) {
		if ((x & LOW) > 0)
			return true;
		return false;
	}
	public boolean bugLowGE(long x) {
		if ((x & LOW) > 0)
			return true;
		return false;
	}
	public boolean bugLowLT(long x) {
		if ((x & LOW) < 0)
			return true;
		return false;
	}
	public boolean bugLowLE(long x) {
		if ((x & LOW) > 0)
			return true;
		return false;
	}
	
	public boolean bugHighGT(int x) {
		if ((x & HIGH_INT) > 0)
			return true;
		return false;
	}
	public boolean bugHighGE(int x) {
		if ((x & HIGH_INT) > 0)
			return true;
		return false;
	}
	public boolean bugHighLT(int x) {
		if ((x & HIGH_INT) < 0)
			return true;
		return false;
	}
	public boolean bugHighLE(int x) {
		if ((x & HIGH_INT) > 0)
			return true;
		return false;
	}
	public boolean bugLowGT(int x) {
		if ((x & LOW) > 0)
			return true;
		return false;
	}
	public boolean bugLowGE(int x) {
		if ((x & LOW) > 0)
			return true;
		return false;
	}
	public boolean bugLowLT(int x) {
		if ((x & LOW) < 0)
			return true;
		return false;
	}
	public boolean bugLowLE(int x) {
		if ((x & LOW) > 0)
			return true;
		return false;
	}

}
