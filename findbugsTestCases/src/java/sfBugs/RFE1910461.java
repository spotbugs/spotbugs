package sfBugs;

public class RFE1910461 {
	
	int f(int x) {
		int m = 10;
		if (x <= 0)
			return m = 8;
		return m*x;
	}

}
