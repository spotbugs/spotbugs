package ghIssues;

import java.util.Iterator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Issue3351 {
	public static class Issue3351SuppressedNoSuchElement implements Iterator<Integer> {

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		@SuppressFBWarnings(value = "IT_NO_SUCH_ELEMENT", justification = "testing bug suppression is not unnecessary")
		public Integer next() {
			return 42;
		}
	}
	
	public static class Issue3351NotSuppressedNoSuchElement implements Iterator<Integer> {
		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Integer next() { // Expecting IT_NO_SUCH_ELEMENT
			return 42;
		}
	}
}
