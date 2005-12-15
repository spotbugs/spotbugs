public class DETest {
	// silly method to sometimes throw exceptions
	public static void x(int i) throws Exception {
		if (i > 0) {
			throw new Exception();
		}
	}

	public static void main(String[] args) {
		try {
			x(0);
		} catch (Exception e1) {
			// nothing here - should be flagged
		} finally {
			try {
				x(1);
			} catch (Exception e2) {
				// nothing here - should also be flagged
			}
		}
	}
}
