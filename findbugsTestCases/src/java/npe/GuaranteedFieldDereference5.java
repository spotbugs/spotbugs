package npe;

public class GuaranteedFieldDereference5 {


	// We should generate a warning that if the if test fails,
	// we are guaranteed to get a dereference when we compute
	// the hashCode
	
	public int doNotReport(int b) {
		if (x == null)
			x = new Object();
		return x.hashCode();
	}

	public GuaranteedFieldDereference5(Object x) {
		this.x = x;
	}
	Object x;

}
