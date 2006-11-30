class NonShortCircuit {
	boolean b;

	boolean arrayDanger(int [] a, int i, int x) {
		return i < a.length & a[i] == x;
	}
	boolean bothBitsFalsePositive(int i) {
		return and((i & 0x1) != 0, (i & 0x2) != 0);
	}
	boolean and(boolean x, boolean y) {
		return x & y;
	}

	void orIt(boolean x, boolean y) {
		x |= y;
		b |= x;
	}

	void andIt(boolean x, boolean y) {
		x &= y;
		b &= x;
	}

	boolean ordered(int x, int y, int z) {
		if (x >= y | y >= z)
			System.out.println("Not ordered");
		return x < y & y < z;
	}

	boolean nonEmpty(Object o[]) {
		return o != null & o.length > 0;
	}

	public static final int BIT0 = 1; // 1st bit

	protected int m_iType;

	public NonShortCircuit(boolean available) {
		m_iType |= available ? BIT0 : 0;
	}
	
	public String f(String tag, String value) {
	  if (tag != null & tag.length() > 0 &&
              value != null && value.length() > 0) 
		  return tag + ":" + value;
	  return "?";
	}
}
