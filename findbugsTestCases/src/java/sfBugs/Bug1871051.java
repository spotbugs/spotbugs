package sfBugs;

public class Bug1871051 {
	public Object clone() {
		return new Bug1871051();
	}
}
