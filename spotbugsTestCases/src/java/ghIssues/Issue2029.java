package ghIssues;

public class Issue2029 {
    public class Inner {
    	void bug(Issue2029 value) throws Throwable {
    		value.finalize();
    	}
    }
}
