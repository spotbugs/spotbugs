package npe;

public class UseCheckUse {
	final Object x;
	UseCheckUse(Object x) {
		this.x = x;
    }
	int f(boolean b) {
		int result = x.hashCode();
		if (x == null) {
            System.out.println("x is null");
		}
		if (b) 
			result *= x.hashCode();
        else
			result += x.hashCode();
		return result;
	}

}
