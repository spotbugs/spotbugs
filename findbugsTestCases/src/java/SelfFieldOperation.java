
public class SelfFieldOperation {
	 int x,y;
	volatile int z;
	boolean volatileFalsePositive() {
        return z == z;
	}
	int f() {
		if (x < x)
            x = y^y;
		if (x != x)
			y = x|x;
		if (x >= x)
            x = y&y; 
		if (y > y)
			y = x-x;
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
