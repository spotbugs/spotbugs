package npe;

public class GuaranteedFieldDereference3 {


	// We should generate a warning that if the if test fails,
	// we are guaranteed to get a dereference when we compute
	// the hashCode

	public int report() {
		if (x != null)
			x = new Object();
		return x.hashCode();
	}

	public int doNotReport() {
		if (x == null)
			x = new Object();
		return x.hashCode();
	}

	public GuaranteedFieldDereference3(Object x) {
		this.x = x;
	}
	Object x;

}
