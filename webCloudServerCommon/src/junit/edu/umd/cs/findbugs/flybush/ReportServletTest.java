package edu.umd.cs.findbugs.flybush;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static edu.umd.cs.findbugs.flybush.FlybushServletTestUtil.createDbIssue;

@SuppressWarnings({"unused"})
public abstract class ReportServletTest extends AbstractFlybushServletTest {
    private List<String> generatedCharts;

    @Override protected void setUp() throws Exception {
        super.setUp();
        generatedCharts = new ArrayList<String>();
    }

    @Override
    protected AbstractFlybushServlet createServlet() {
        return new ReportServlet() {
            @Override
            protected void showChartImg(HttpServletResponse resp, String url) throws IOException {
                generatedCharts.add(url);
                System.out.println(url);
            }
        };
    }

    public void testGraphEvalsByUser() throws Exception {
        DbIssue foundIssue = createDbIssue("fad2", persistenceHelper);
        DbEvaluation eval1 = createEvaluation(foundIssue, "someone", 100);
        DbEvaluation eval2 = createEvaluation(foundIssue, "someone-else", 200);
        DbEvaluation eval3 = createEvaluation(foundIssue, "someone", 300);
        DbEvaluation eval4 = createEvaluation(foundIssue, "someone3", 300);
        DbEvaluation eval5 = createEvaluation(foundIssue, "someone3", 300);
        DbEvaluation eval6 = createEvaluation(foundIssue, "someone3", 300);
        foundIssue.addEvaluation(eval1);
        foundIssue.addEvaluation(eval2);
        foundIssue.addEvaluation(eval3);
        foundIssue.addEvaluation(eval4);
        foundIssue.addEvaluation(eval5);
        foundIssue.addEvaluation(eval6);

        getPersistenceManager().makePersistent(foundIssue);

        executeGet("/stats");

        String url = generatedCharts.get(0);
        assertTrue(url.contains("someone-else|someone|someone3"));
        assertTrue(url.contains("chd=e:ApBSB7")); // encoded data... not very test friendly
    }

    public void testGraphEvalsByDate() throws Exception {
        DbIssue foundIssue = createDbIssue("fad2", persistenceHelper);
        DbEvaluation eval1 = createEvaluation(foundIssue, "someone", days(2));
        DbEvaluation eval2 = createEvaluation(foundIssue, "someone-else", days(2));
        DbEvaluation eval3 = createEvaluation(foundIssue, "someone", days(3));
        DbEvaluation eval4 = createEvaluation(foundIssue, "someone3", days(3));
        DbEvaluation eval5 = createEvaluation(foundIssue, "someone3", days(3));
        DbEvaluation eval6 = createEvaluation(foundIssue, "someone3", days(5));
        foundIssue.addEvaluation(eval1);
        foundIssue.addEvaluation(eval2);
        foundIssue.addEvaluation(eval3);
        foundIssue.addEvaluation(eval4);
        foundIssue.addEvaluation(eval5);
        foundIssue.addEvaluation(eval6);

        getPersistenceManager().makePersistent(foundIssue);

        executeGet("/stats");

        String url = generatedCharts.get(1);
        assertTrue(url.contains("someone-else|someone|someone3"));
        assertTrue(url.contains("chd=e:ApBSB7")); // encoded data... not very test friendly
    }

    private long days(int days) {
        return days*86400*1000;
    }
}
