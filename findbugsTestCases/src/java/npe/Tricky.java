package npe;

public class Tricky {
	boolean sameLengthArrays(int a[], int b[]) {
		// TODO: if a is null and b is nonnull, this
		// will get a NPE
		if (a == null && b == null) return true;
		
		if (b == null && a == null) return true;
		
		if (b != null) return a.length == b.length;
		return false;
	}

}
