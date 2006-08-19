package npe;


public class GuaranteedDereferenceInteractionWithAssertionMethods {

	public Object x;

	public boolean b;

	public int falsePositive() {

		if (x == null)
			System.out.println("x is null");
		if (b) {
			x = bar();
			checkForError();
		}

		return x.hashCode();
	}

	public void checkForError() {

	}

	public Object bar() {
		return new Object();
	}

}
