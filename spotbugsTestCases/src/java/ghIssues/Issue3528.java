package ghIssues;

public class Issue3528 {
	@Override
	public int hashCode() {
		return 42;
	}
	
	@Override
	public boolean equals(Object obj) {
	    boolean checkEqual = (obj instanceof Issue3528);
	    System.out.println(checkEqual);
	    return true; // expecting EQ_ALWAYS_TRUE here
	}
}
