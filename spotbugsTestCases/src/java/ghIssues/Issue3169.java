package ghIssues;

import junit.framework.TestCase;

public class Issue3169 extends TestCase {
	private String x;
	private int y;
	
	public Issue3169() {
		y = 12;
	}

	@Override
	protected void setUp() throws Exception {
		x = "42";
	}
	
	public int doStuff() {
		return x.length() * y;
	}
}
