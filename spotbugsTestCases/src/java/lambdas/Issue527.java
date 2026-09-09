package lambdas;

public class Issue527 {

	public void errorOnLambda() {
		Object o = getObject();
		if (o != null) {
			useRunnable(() -> toString());
		}
		o.hashCode();
		Object o2 = getObject();
		if (o2 != null) {
			useRunnable(() -> toString());
		}
		o2.hashCode();
	}

	public Object getObject() {
		return null;
	}

	public void useRunnable(Runnable listener) {
	}
}
