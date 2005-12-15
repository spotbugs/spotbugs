public class ITest {

	public interface A {
	}

	public interface B extends A {
	}

	public boolean compareDoNotReport(A first, B second) {
		// It is possible that "first" is an instance of
		// a class that implements B. Therefore, it
		// is possible that the comparision can be true,
		// so we shouldn't report.
		return first.equals(second);
	}
}
