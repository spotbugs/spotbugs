
class NonShortCircuit {
  boolean and( boolean x, boolean y) {
	return x & y;
	}
  boolean ordered(int x, int y, int z) {
	if (x >= y | y >= z) System.out.println("Not ordered");
	return x < y & y < z;
	}
  boolean nonEmpty(Object o[]) {
	return o != null & o.length > 0;
	}
}
