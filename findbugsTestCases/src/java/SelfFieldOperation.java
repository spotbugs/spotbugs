
public class SelfFieldOperation {
	int x,y;
	volatile int z;
	boolean volatileFalsePositive() {
        return z == z;
	}
	int f(int x, int y) {
		if (x < x)
            x = y^y;
		if (x != x)
			y = x|x;
		if (x >= x)
            x = y&y; 
		return x; 
	}

    Integer a, b;
	boolean e() {
		return a.equals(a);
	}
    int c() {
		return a.compareTo(a);
	}


}
