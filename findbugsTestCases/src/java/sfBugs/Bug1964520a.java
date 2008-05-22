package sfBugs;

public class Bug1964520a {

	

	private Superclass something;

	public void setSomething(Superclass object) {
		this.something = object;
		if (something instanceof Subclass && ((Subclass) something).bla()) // this line is bug-annotated by FindBugs
		{
			((Subclass) something).foo();
		}
	}

	private static class Superclass {
		//
	}

	private static class Subclass extends Superclass {
		public boolean bla() {
			return true;
		}

		public void foo() {
			//
		}
	}

}
