package edu.umd.cs.findbugs.flybush;

public class WebPageServletTest extends AbstractFlybushServletTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected AbstractFlybushServlet createServlet() {
        return new WebPageServlet();
    }

    public void testIssuesPopupZeroEvals() throws Exception {
        DbIssue foundIssue = createDbIssue("fad2");

        getPersistenceManager().makePersistent(foundIssue);

        executeGet("/issues/fad2?embed");
        String response = outputCollector.toString();
        System.err.println("HTML RESPONSE:\n" + response);
        assertTrue(response.contains("No comments have been submitted for this issue"));
    }

    public void testIssuesPopupNoSuchIssue() throws Exception {
        executeGet("/issues/fadfad?embed");
        String response = outputCollector.toString();
        System.err.println("HTML RESPONSE:\n" + response);
        assertTrue(response.contains("This issue has not been submitted to the"));
    }

    public void testIssuesPopupOneEval() throws Exception {
        DbIssue foundIssue = createDbIssue("fad2");
        DbEvaluation eval = createEvaluation(foundIssue, "someone", 100, "WILL_FIX", "first comment");

        getPersistenceManager().makePersistent(foundIssue);

        executeGet("/issues/fad2?embed");
        String response = outputCollector.toString();
        System.err.println("HTML RESPONSE:\n" + response);
        assertTrue(response.contains("someone"));
        assertTrue(response.contains("WILL_FIX"));
        assertTrue(response.contains("first comment"));
    }

    public void testIssuesPopupMultipleEvalsSamePerson() throws Exception {
        DbIssue foundIssue = createDbIssue("fad2");
        DbEvaluation eval1 = createEvaluation(foundIssue, "someone", 100, "WILL_FIX", "first");
        DbEvaluation eval2 = createEvaluation(foundIssue, "someone", 200, "NOT_A_BUG", "second");

        getPersistenceManager().makePersistent(foundIssue);

        executeGet("/issues/fad2?embed");
        String response = outputCollector.toString();
        System.err.println("HTML RESPONSE:\n" + response);
        assertTrue(response.contains("someone"));
        assertTrue(response.contains("NOT_A_BUG"));
        assertTrue(response.contains("second"));

        assertFalse(response.contains("WILL_FIX"));
        assertFalse(response.contains("first"));
    }

    public void testIssuesPopupMultipleEvaluators() throws Exception {
        DbIssue foundIssue = createDbIssue("fad2");
        DbEvaluation eval1 = createEvaluation(foundIssue, "someone", 100, "WILL_FIX", "first");
        DbEvaluation eval2 = createEvaluation(foundIssue, "otherperson", 200, "NOT_A_BUG", "second");

        getPersistenceManager().makePersistent(foundIssue);

        executeGet("/issues/fad2?embed");
        String response = outputCollector.toString();
        System.err.println("HTML RESPONSE:\n" + response);
        assertTrue(response.contains("someone"));
        assertTrue(response.contains("NOT_A_BUG"));
        assertTrue(response.contains("second"));

        assertTrue(response.contains("WILL_FIX"));
        assertTrue(response.contains("first"));

        // make sure they're in reverse order by date
        assertTrue(response.indexOf("WILL_FIX") > response.indexOf("NOT_A_BUG"));
    }
}
