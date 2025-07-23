package ghIssues;

public interface Issue1988 {
	public default int check() {
		return compute();
	}

	private int compute() {
		return 42;
	}
 }
