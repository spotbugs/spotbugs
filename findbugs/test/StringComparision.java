

class StringComparision {
	int x,y;
	public String toString() {
		return x + "," + y;
		}
	public boolean isOrigin() {
		return toString() == "0,0";
		}
	public boolean betterIsOrigin() {
		return toString().intern() == "0,0";
		}

	public boolean compareBool(Boolean a, Boolean b) { return a == b; }

	}
