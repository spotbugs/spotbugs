package edu.umd.cs.findbugs.flybush;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.googlecode.charts4j.AxisLabelsFactory;
import com.googlecode.charts4j.BarChart;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.Data;
import com.googlecode.charts4j.DataEncoding;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.Line;
import com.googlecode.charts4j.LineChart;
import com.googlecode.charts4j.Plots;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ReportServlet extends AbstractFlybushServlet {
    private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT);

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
        Map<String,Integer> totalCountByUser = new HashMap<String, Integer>();
        Map<String,Integer> issueCountByUser = new HashMap<String, Integer>();
        Multimap<String, String> issuesByUser = Multimaps.newSetMultimap(Maps.<String, Collection<String>>newHashMap(), new Supplier<Set<String>>() {
            public Set<String> get() {
                return Sets.newHashSet();
            }
        });
        Map<Long,Integer> evalsByWeek = Maps.newHashMap();

        for (DbEvaluation eval : evals) {
            String email = eval.getEmail();
            if (email == null)
                continue;

            issuesByUser.put(email, eval.getIssue().getHash());
            increment(totalCountByUser, email);
            issueCountByUser.put(email, issuesByUser.get(email).size());

            long beginningOfWeek = getBeginningOfWeekInMillis(eval.getWhen());
            increment(evalsByWeek, beginningOfWeek);
        }

        BarChart evalsByUserChart = buildByUserChart(totalCountByUser, issueCountByUser);

        LineChart evalsOverTimeChart = createEvalsByWeekChart(evalsByWeek);
        evalsOverTimeChart.setTitle("Evaluations Submitted");

        resp.setStatus(200);

        resp.getOutputStream().print("<html>" +
                                     "<head><title>FindBugs Cloud Stats</title></head>" +
                                     "<body>");
        showChartImg(resp, evalsOverTimeChart.toURLString());
        resp.getOutputStream().print("<br><br>");
        showChartImg(resp, evalsByUserChart.toURLString());
    }

    private <E> void increment(Map<E, Integer> map, E key) {
        Integer oldCount = map.get(key);
        if (oldCount == null) oldCount = 0;
        map.put(key, oldCount + 1);
    }

    private LineChart createEvalsByWeekChart(Map<Long, Integer> evalsByWeek) {
        long first = Collections.min(evalsByWeek.keySet());
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(first);
        long last = Collections.max(evalsByWeek.keySet());
        List<Double> byWeekData = new ArrayList<Double>();
        List<String> labels = Lists.newArrayList();
        int maxPerWeek = Collections.max(evalsByWeek.values());
        for (; cal.getTimeInMillis() <= last; cal.add(Calendar.DAY_OF_MONTH, 7)) {
            Integer count = evalsByWeek.get(cal.getTimeInMillis());
            count = count == null ? 0 : count;
            byWeekData.add(count * 100.0 / maxPerWeek);
            labels.add(DATE_FORMAT.format(new Date(cal.getTimeInMillis())));
        }

        Line line = Plots.newLine(Data.newData(byWeekData));
        line.setFillAreaColor(Color.BEIGE);
        LineChart chart = GCharts.newLineChart(line);
        chart.setDataEncoding(DataEncoding.TEXT);
        chart.setSize(600, 250);
        chart.setGrid(100, 10 / (maxPerWeek / 100.0), 4, 1);
        chart.addYAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, maxPerWeek));
        chart.addXAxisLabels(AxisLabelsFactory.newAxisLabels(labels));
        return chart;
    }

    private void resetToMidnight(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private long getBeginningOfWeekInMillis(long when) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(when);
        Calendar weekCal = Calendar.getInstance();
        weekCal.set(Calendar.WEEK_OF_YEAR, cal.get(Calendar.WEEK_OF_YEAR));
        weekCal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        resetToMidnight(weekCal);
        weekCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        return weekCal.getTimeInMillis();
    }

    private BarChart buildByUserChart(Map<String, Integer> totalCountByUser,
                                      Map<String, Integer> issueCountByUser) {
        List<String> labels = Lists.newArrayList();
        List<Double> totals = Lists.newArrayList();
        List<Double> issues = Lists.newArrayList();
        int max = Collections.max(totalCountByUser.values());
        for (Entry<String, Integer> entry : sortEntries(totalCountByUser.entrySet())) {
            String email = entry.getKey();
            int issueCount = issueCountByUser.get(email);
            int evalCount = entry.getValue();
            labels.add(email);
            totals.add((evalCount - issueCount) * 100.0 / max);
            issues.add(issueCount * 100.0 / max);
        }

        Collections.reverse(totals);
        Collections.reverse(issues);
        BarChart chart = GCharts.newBarChart(Plots.newBarChartPlot(Data.newData(issues), Color.DARKORCHID, "New evals"),
                                             Plots.newBarChartPlot(Data.newData(totals), Color.ORCHID, "Eval updates"));

        chart.setDataStacked(true);
        chart.setGrid(20.0 / (max / 100.0), 100, 4, 1);
        chart.addYAxisLabels(AxisLabelsFactory.newAxisLabels(labels));
        chart.addXAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, max));
        chart.setBarWidth(BarChart.AUTO_RESIZE);
        chart.setSize(600, 500);
        chart.setHorizontal(true);
        chart.setDataEncoding(DataEncoding.TEXT);
        return chart;
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
