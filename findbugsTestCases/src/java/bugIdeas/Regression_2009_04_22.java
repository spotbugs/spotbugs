package bugIdeas;

public class Regression_2009_04_22 {

	
	Object alwaysNonNull() {
		return "X";
	}
	Object sometimesNull() {
		if (Math.random() > 0.5)
			return null;
		return "Y";
	}
	
	void check() {
		Object x = alwaysNonNull();
		if (x == null) 
			System.out.println("huh");
		Object y = sometimesNull();
		System.out.println(y.hashCode());
		if (y == null) 
			System.out.println("huh");
	}
}
