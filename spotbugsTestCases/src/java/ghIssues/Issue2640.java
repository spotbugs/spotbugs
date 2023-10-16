package ghIssues;

public class Issue2640 {
    public void testUnreachableCode() {
        org.junit.Assert.fail();
        System.err.println("Not reachable code");
    }
}
