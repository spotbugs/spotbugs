package bugIdeas;

public class Ideas_2009_01_05 {
	
	Object x;
	
	// like to treat this as a method that must have a nonnull parameter
	void pleaseGiveMeNonnull(Object x) {
		if (x == null)
			throw new NullPointerException();
		this.x = x;
	}
	
	int getHash() {
		return x.hashCode();
	}
	void test() {
		pleaseGiveMeNonnull(null); // like to generate a warning here
	}

}
