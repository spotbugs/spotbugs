package sfBugs;

public class RFE1910461 {
	

	boolean high(boolean b1, boolean b2, boolean b3) {
		if (b1)
			return b1 = b2;
		else return b2=b3;
	}
	int medium(int x) {
		int m = 10;
		if (x <= 0)
			return m = 8;
		return m*x;
	}
	int low(int x) {
		int m = 10;
		if (x > 0)
			return m*x;
		return m*=x;
	}
	String lowString(int x) {
		String s = "foo";
		if (x < 0)
			return s+=x;
		return s;
	}
	String fpString(int x) {
		String s = "foo";
		if (x >= 0)
			return s;
		return s+=x;
	}

}
