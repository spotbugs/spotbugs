package ghIssues;

public class Issue3634 {
	static Issue3634 DEFAULT = new Issue3634();
	
	volatile String field = Issue3634.class.getCanonicalName();
	
	public Issue3634 parent;
	public int counter;
	
	public void fieldLock() {
		synchronized (field) {
			field.notifyAll();
		}
	}
	
	public static void fieldOfStaticFieldLock() {
		synchronized (DEFAULT.field) {
			DEFAULT.field.notifyAll();
		}
	}
	
	public void fieldOfFieldLock() {
		synchronized (parent.field) {
			parent.field.notifyAll();
		}
	}
	
	public void chainedFieldsLock() {
		synchronized (parent.parent.parent.parent.parent.parent.parent.field) {
			parent.parent.parent.parent.parent.parent.parent.field.notifyAll();
		}
	}
	
	public static void staticFieldLock() {
		synchronized (DEFAULT) {
			DEFAULT.notifyAll();
		}
	}	
	
	public void fieldLockWithMutation() {
		synchronized (field) {
			counter++;
			DEFAULT.field.notifyAll();
		}
	}
	
	public void chainedFieldsLockWithMutation() {
		synchronized (parent.parent.parent.parent.parent.parent.parent.field) {
			field = null;
			parent.parent.parent.parent.parent.parent.parent.field.notifyAll();
		}
	}
}
