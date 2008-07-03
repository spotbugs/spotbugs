package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug2003243 {

	String field = Bug2003243.class.getCanonicalName();
	static Bug2003243 record = new Bug2003243();

	@NoWarning("MWN")
	public void foo() {
		synchronized (record.field) {
			record.field.notifyAll(); // There should not be an MWN violation here
		}
	}
	
	@NoWarning("MWN")
	public void different() {
		Object lock = record.field;
		synchronized (lock) {
			lock.notifyAll();
		}
	}
}
