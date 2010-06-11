package edu.umd.cs.findbugs.flybush;

import com.googlecode.charts4j.AxisLabelsFactory;
import com.googlecode.charts4j.BarChart;
import com.googlecode.charts4j.Data;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.Plots;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ReportServlet extends AbstractFlybushServlet {
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

        String uri = req.getRequestURI();
        PersistenceManager pm = getPersistenceManager();
        try {
            if (uri.equals("/stats")) {
                showStats(req, resp, pm);

            } else {
                show404(resp);
            }
        } finally {
            pm.close();
        }
	}

    @SuppressWarnings({"unchecked"})
    private void showStats(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm)
            throws IOException {
        List<DbEvaluation> evals = (List<DbEvaluation>) pm.newQuery("select from " + persistenceHelper.getDbEvaluationClass().getName()
                    + " order by when").execute();
        Map<String,Integer> countByUser = new HashMap<String, Integer>();
        for (DbEvaluation eval : evals) {
            String email = eval.getEmail();
            if (email == null)
                continue;
            Integer count = countByUser.get(email);
            if (count == null) count = 0;
            countByUser.put(email, count + 1);
        }

        List<String> labels = new ArrayList<String>();
        List<Integer> values = new ArrayList<Integer>();
        int max = 0;
        for (Entry<String, Integer> entry : sortEntries(countByUser.entrySet())) {
            String email = entry.getKey();
            int count = entry.getValue();
            labels.add(email + "(" + count + ")");
            values.add(count);
            if (count > max)
                max = count;
        }

        resp.setStatus(200);
        Collections.reverse(values);
        BarChart chart = GCharts.newBarChart(Plots.newBarChartPlot(Data.newData(values)));
        chart.addYAxisLabels(AxisLabelsFactory.newAxisLabels(labels));
        chart.addXAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, max));
        chart.setBarWidth(BarChart.AUTO_RESIZE);
        chart.setSize(600, 500);
        chart.setHorizontal(true);
        chart.setTitle("Total Evaluations by User");
        showChartImg(resp, chart.toURLString());
    }

    private List<Entry<String, Integer>> sortEntries(Collection<Entry<String, Integer>> entries) {
        List<Entry<String, Integer>> result = new ArrayList<Entry<String, Integer>>(entries);
        Collections.sort(result, new Comparator<Entry<String, Integer>>() {
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                int numbers = o1.getValue().compareTo(o2.getValue());
                if (numbers != 0)
                    return numbers;
                // fallback to alpha order by email
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        return result;
    }

    /** protected for testing */
    protected void showChartImg(HttpServletResponse resp, String url) throws IOException {
        resp.getOutputStream().print("<img src='" + url + "'>");
    }

    @Override
    protected void handlePost(PersistenceManager pm, HttpServletRequest req, HttpServletResponse resp, String uri)
            throws IOException {
    }
}
