package deref;

// Based on code from org.apache.tools.zip.ZipEntry
public class UnconditionalDerefFalsePositive {
	int x;

	UnconditionalDerefFalsePositive(int x) {
		this.x = x;
	}

	UnconditionalDerefFalsePositive(UnconditionalDerefFalsePositive other) {
		this.x = other.x;
	}

	public Object clone() {
		UnconditionalDerefFalsePositive e = null;
		try {
			e = new UnconditionalDerefFalsePositive(
					(UnconditionalDerefFalsePositive) super.clone());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		e.x = x + 1;
		return e;
	}

}
