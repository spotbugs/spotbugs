package edu.umd.cs.findbugs.flybush;

import org.mockito.Mockito;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        DbIssue foundIssue1 = createDbIssue("fad2", persistenceHelper);
        DbIssue foundIssue2 = createDbIssue("fad3", persistenceHelper);
        createEvaluation(foundIssue1, "someone", 100);
        createEvaluation(foundIssue1, "someone-else", 200);
        createEvaluation(foundIssue2, "someone", 300);
        createEvaluation(foundIssue2, "someone", 300);
        createEvaluation(foundIssue1, "someone3", 300);
        createEvaluation(foundIssue1, "someone3", 300);
        createEvaluation(foundIssue1, "someone3", 300);
        createEvaluation(foundIssue2, "someone3", 300);

        getPersistenceManager().makePersistentAll(foundIssue1, foundIssue2);

        executeGet("/stats");

        String url = generatedCharts.get(2);
        checkParam(url, "chxl", "0:|someone-else|someone|someone3");
        checkParam(url, "chd", "t:50.0,50.0,25.0|50.0,25.0,0.0");
    }

    public void testGraphEvalsByUserOver100() throws Exception {
        DbIssue foundIssue = createDbIssue("fad2", persistenceHelper);
        createEvaluation(foundIssue, "someone", 100);
        createEvaluation(foundIssue, "someone-else", 200);
        createEvaluation(foundIssue, "someone", 300);

        for (int i = 0; i < 120; i++) {
            foundIssue.addEvaluation(createEvaluation(foundIssue, "lots", 400));
        }

        getPersistenceManager().makePersistent(foundIssue);

        executeGet("/stats");

        String url = generatedCharts.get(2);
        checkParam(url, "chxl", "0:|someone-else|someone|lots");
        checkParam(url, "chd", "t:0.8,0.8,0.8|99.2,0.8,0.0");
    }

    public void testGraphEvalsByDate() throws Exception {
        DbIssue foundIssue1 = createDbIssue("fad2", persistenceHelper);
        DbIssue foundIssue2 = createDbIssue("fad3", persistenceHelper);
        DbIssue foundIssue3 = createDbIssue("fad4", persistenceHelper);
        // week 1
        createEvaluation(foundIssue1, "someone", days(2));
        createEvaluation(foundIssue1, "someone-else", days(2));
        createEvaluation(foundIssue1, "someone", days(3));

        // week 2
        createEvaluation(foundIssue2, "someone3", days(9));
        createEvaluation(foundIssue1, "someone3", days(10));

        // week 4
        createEvaluation(foundIssue3, "someone3", days(24));
        createEvaluation(foundIssue1, "someone3", days(25));

        getPersistenceManager().makePersistentAll(foundIssue1, foundIssue2, foundIssue3);

        executeGet("/stats");

        String url = generatedCharts.get(0);
        checkParam(url, "chxl", "1:|3/14/10|3/21/10|3/28/10|4/4/10|4/11/10");
        checkParam(url, "chd", "t:0.0,100.0,66.7,0.0,66.7|0.0,33.3,33.3,0.0,33.3|0.0,66.7,33.3,0.0,0.0");
    }

    public void testCumulativeTimelineGraph() throws Exception {
        DbIssue foundIssue1 = createDbIssue("fad2", persistenceHelper);
        DbIssue foundIssue2 = createDbIssue("fad3", persistenceHelper);
        DbIssue foundIssue3 = createDbIssue("fad4", persistenceHelper);
        // week 1
        createEvaluation(foundIssue1, "someone", days(2));
        createEvaluation(foundIssue1, "someone-else", days(2));
        createEvaluation(foundIssue1, "someone", days(3));

        // week 2
        createEvaluation(foundIssue2, "someone3", days(9));
        createEvaluation(foundIssue1, "someone3", days(10));

        // week 4
        createEvaluation(foundIssue3, "someone3", days(24));
        createEvaluation(foundIssue1, "someone3", days(25));

        getPersistenceManager().makePersistentAll(foundIssue1, foundIssue2, foundIssue3);

        executeGet("/stats");

        String url = generatedCharts.get(1);
        checkParam(url, "chxl", "2:|3/14/10|3/21/10|3/28/10|4/4/10|4/11/10");
        checkParam(url, "chd", "t:0.0,42.9,71.4,71.4,100.0|0.0,14.3,28.6,28.6,42.9|0.0,66.7,100.0,100.0,100.0");
    }

    public void testGraphIssuesByEvaluatorCount() throws Exception {
        DbIssue foundIssue1 = createDbIssue("fad2", persistenceHelper);
        DbIssue foundIssue2 = createDbIssue("fad3", persistenceHelper);
        DbIssue foundIssue3 = createDbIssue("fad4", persistenceHelper);
        DbIssue foundIssue4 = createDbIssue("fad5", persistenceHelper);
        DbIssue foundIssue5 = createDbIssue("fad6", persistenceHelper);
        DbIssue foundIssue6 = createDbIssue("fad7", persistenceHelper);
        createEvaluation(foundIssue1, "a", 100);
        createEvaluation(foundIssue1, "b", 200);
        createEvaluation(foundIssue1, "c", 300);
        createEvaluation(foundIssue1, "d", 300);
        createEvaluation(foundIssue1, "e", 300);
        createEvaluation(foundIssue1, "e", 300); // duplicate

        createEvaluation(foundIssue2, "f", 300);
        createEvaluation(foundIssue2, "g", 300);
        createEvaluation(foundIssue2, "h", 300);

        createEvaluation(foundIssue6, "k", 300);
        createEvaluation(foundIssue6, "l", 300);
        createEvaluation(foundIssue6, "m", 300);

        createEvaluation(foundIssue3, "i", 300);

        createEvaluation(foundIssue4, "j", 100);

        createEvaluation(foundIssue5, "j", 100);

        getPersistenceManager().makePersistentAll(foundIssue1, foundIssue2, foundIssue3,
                                                  foundIssue4, foundIssue5, foundIssue6);

        executeGet("/stats");

        String url = generatedCharts.get(3);
        checkParam(url, "chxl", "1:|1|2|3|4|5|2:|No. of evaluators");
        checkParam(url, "chd", "t:100.0,0.0,66.7,0.0,33.3");
    }

    public void testGraphUserEvalsByDate() throws Exception {
        DbIssue foundIssue1 = createDbIssue("fad2", persistenceHelper);
        DbIssue foundIssue2 = createDbIssue("fad3", persistenceHelper);
        DbIssue foundIssue3 = createDbIssue("fad4", persistenceHelper);
        // week 1
        createEvaluation(foundIssue1, "someone", days(2));
        createEvaluation(foundIssue1, "someone-else", days(2));
        createEvaluation(foundIssue1, "someone", days(3));

        // week 2
        createEvaluation(foundIssue2, "someone3", days(9));
        createEvaluation(foundIssue1, "someone3", days(10));

        // week 4
        createEvaluation(foundIssue3, "someone3", days(24));
        createEvaluation(foundIssue1, "someone3", days(25));
        createEvaluation(foundIssue2, "someone3", days(25));

        getPersistenceManager().makePersistentAll(foundIssue1, foundIssue2, foundIssue3);

        prepareRequestAndResponse("/stats", null);
        Mockito.when(mockRequest.getParameter("user")).thenReturn("someone3");
        servlet.doGet(mockRequest, mockResponse);

        String url = generatedCharts.get(0);
        checkParam(url, "chxl", "1:|3/21/10|3/28/10|4/4/10|4/11/10");
        checkParam(url, "chd", "t:0.0,66.7,0.0,100.0|0.0,66.7,0.0,33.3");
    }

    public void testGraphUserEvalsByDateJustOneWeek() throws Exception {
        DbIssue foundIssue1 = createDbIssue("fad2", persistenceHelper);
        // week 1
        createEvaluation(foundIssue1, "someone", days(2));
        createEvaluation(foundIssue1, "someone2", days(2));

        // week 2
        createEvaluation(foundIssue1, "someone2", days(9));

        getPersistenceManager().makePersistentAll(foundIssue1);

        prepareRequestAndResponse("/stats", null);
        Mockito.when(mockRequest.getParameter("user")).thenReturn("someone");
        servlet.doGet(mockRequest, mockResponse);

        String url = generatedCharts.get(0);
        checkParam(url, "chxl", "1:|3/14/10|3/21/10");
        checkParam(url, "chd", "t:0.0,100.0|0.0,100.0");
    }

    public void testUserSelectionComboBox() throws Exception {
        DbIssue foundIssue1 = createDbIssue("fad2", persistenceHelper);
        DbIssue foundIssue2 = createDbIssue("fad3", persistenceHelper);
        DbIssue foundIssue3 = createDbIssue("fad4", persistenceHelper);
        // week 1
        createEvaluation(foundIssue1, "someone", days(2));
        createEvaluation(foundIssue1, "someone-else", days(2));
        createEvaluation(foundIssue1, "someone", days(3));

        // week 2
        createEvaluation(foundIssue2, "someone3", days(9));
        createEvaluation(foundIssue1, "someone3", days(10));

        // week 4
        createEvaluation(foundIssue3, "someone3", days(24));
        createEvaluation(foundIssue1, "someone3", days(25));
        createEvaluation(foundIssue2, "someone3", days(25));

        getPersistenceManager().makePersistentAll(foundIssue1, foundIssue2, foundIssue3);

        executeGet("/stats");

        // list of recent evals
        // upload project name, invocation end time
        // store invocation per issue
        // remove comment field
        

        int last = 0;
        String page = outputCollector.toString();
        for (String email : Arrays.asList("someone", "someone-else", "someone3")) {
            last = page.indexOf("<option value=\"" + email + "\">" + email + "</option>", last);
            assertTrue("could not find combo box item for " + email + ": " + page,
                       last != -1);
        }
    }

    // =============================== end of tests ==================================
    
    private void checkParam(String url, String pname, String expectedValue)
            throws UnsupportedEncodingException {
        Map<String, List<String>> params = getParams(url);
        List<String> pvalue = params.get(pname);
        assertNotNull("Expected value for " + pname, pvalue);
        assertEquals("Expected 1 value for " + pname, 1, pvalue.size());
        assertEquals(pname, expectedValue, pvalue.get(0));
    }

    private Map<String, List<String>> getParams(String url) throws UnsupportedEncodingException {
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        String[] urlParts = url.split("\\?");
        if (urlParts.length > 1) {
            String query = urlParts[1];
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                String key = URLDecoder.decode(pair[0], "UTF-8");
                String value = URLDecoder.decode(pair[1], "UTF-8");
                List<String> values = params.get(key);
                if (values == null) {
                    values = new ArrayList<String>();
                    params.put(key, values);
                }
                values.add(value);
            }
        }
        return params;
    }

    /** starts at march 20, 2010 */
    private long days(int days) {
        return (1269043200L + (days*86400L))*1000;
    }
}
