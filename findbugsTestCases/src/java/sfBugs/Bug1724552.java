package sfBugs;

public class Bug1724552 {

	private int value;

	public void doBug() {

		int value = 5;

		value = 10;

		this.value = value;
	}

}
