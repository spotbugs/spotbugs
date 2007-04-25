package npe;

public class TrickyLoop {

	// The NPE exception here is mandidated only if the loop has non-zero trip count
	// Should we report this one?
	public int f(Object x, int y) {
		if (x == null) {
			System.out.println("x is null");
		}
		int result = 0;
		for(int i = 0; i < y; i++)
			result += x.hashCode(); // TODO: report NPE here
		return result;
	}

}
