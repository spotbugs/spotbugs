package edu.umd.cs.findbugs.flybush;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.googlecode.charts4j.AxisLabels;
import com.googlecode.charts4j.AxisLabelsFactory;
import com.googlecode.charts4j.AxisStyle;
import com.googlecode.charts4j.AxisTextAlignment;
import com.googlecode.charts4j.BarChart;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.Data;
import com.googlecode.charts4j.DataEncoding;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.Line;
import com.googlecode.charts4j.LineChart;
import com.googlecode.charts4j.Plots;
import org.apache.commons.lang.StringEscapeUtils;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
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

    private void showStats(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm)
            throws IOException {
        if (req.getParameter("user") != null) {
            showUserStats(resp, pm, req.getParameter("user"));
        } else {
            showSummaryStats(resp, pm);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void showUserStats(HttpServletResponse resp, PersistenceManager pm, String email) throws IOException {
        Query query = pm.newQuery("select from " + persistenceHelper.getDbEvaluationClass().getName()
                                  + " where email == :email order by when");
        List<DbEvaluation> evals = (List<DbEvaluation>) query.execute(email);
        if (evals.isEmpty()) {
            setResponse(resp, 404, "No such user");
        }
        Map<Long, Integer> evalsPerWeek = Maps.newHashMap();
        for (DbEvaluation eval : evals) {
            long beginningOfWeek = getBeginningOfWeekInMillis(eval.getWhen());
            increment(evalsPerWeek, beginningOfWeek);
        }
        query.closeAll();

        int maxEvalsPerWeek = Collections.max(evalsPerWeek.values());
        List<Double> data = Lists.newArrayList();
        List<String> labels = Lists.newArrayList();
        for (Calendar cal : iterateByWeek(evalsPerWeek.keySet())) {
            Integer evalsThisWeek = evalsPerWeek.get(cal.getTimeInMillis());
            if (evalsThisWeek == null)
                evalsThisWeek = 0;
            data.add(evalsThisWeek * 100.0 / maxEvalsPerWeek);
            labels.add(DATE_FORMAT.format(new Date(cal.getTimeInMillis())));
        }

        LineChart chart = GCharts.newLineChart(Plots.newLine(Data.newData(data)));
        chart.addXAxisLabels(AxisLabelsFactory.newAxisLabels(labels));
        chart.addYAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, maxEvalsPerWeek));
        chart.setTitle("Evaluations over Time - " + email);
        chart.setDataEncoding(DataEncoding.TEXT);
        chart.setSize(800, 350);

        resp.setStatus(200);
        resp.getOutputStream().print(
                "<html>" +
                "<head><title>" + StringEscapeUtils.escapeHtml(email) + " - FindBugs Cloud Stats</title></head>" +
                "<body>");

        showChartImg(resp, chart.toURLString());
    }

    @SuppressWarnings({"unchecked"})
    private void showSummaryStats(HttpServletResponse resp, PersistenceManager pm) throws IOException {
        Query query = pm.newQuery("select from "
                                  + persistenceHelper.getDbEvaluationClass().getName()
                                  + " order by when");
        List<DbEvaluation> evals = (List<DbEvaluation>) query.execute();
        Map<String,Integer> totalCountByUser = Maps.newHashMap();
        Map<String,Integer> issueCountByUser = Maps.newHashMap();
        Multimap<String, String> issuesByUser = Multimaps.newSetMultimap(Maps.<String, Collection<String>>newHashMap(),
                                                                         new Supplier<Set<String>>() {
                                                                             public Set<String> get() {
                                                                                 return Sets.newHashSet();
                                                                             }
                                                                         });
        Map<Long, Integer> evalsByWeek = Maps.newHashMap();
        Map<Long, Integer> issueCountByWeek = Maps.newHashMap();
        Map<Long, Integer> userCountByWeek = Maps.newHashMap();
        Set<String> seenIssues = Sets.newHashSet();
        Set<String> seenUsers = Sets.newHashSet();

        for (DbEvaluation eval : evals) {
            String email = eval.getEmail();
            if (email == null)
                continue;

            String issueHash = eval.getIssue().getHash();
            issuesByUser.put(email, issueHash);
            increment(totalCountByUser, email);
            issueCountByUser.put(email, issuesByUser.get(email).size());

            long beginningOfWeek = getBeginningOfWeekInMillis(eval.getWhen());
            increment(evalsByWeek, beginningOfWeek);
            seenIssues.add(issueHash);
            issueCountByWeek.put(beginningOfWeek, seenIssues.size());
            seenUsers.add(email);
            userCountByWeek.put(beginningOfWeek, seenUsers.size());
        }
        query.closeAll();

        // build charts
        BarChart histogram = buildEvaluatorsHistogram(issuesByUser);

        BarChart evalsByUserChart = buildByUserChart(totalCountByUser, issueCountByUser);

        LineChart evalsOverTimeChart = createTimelineChart(evalsByWeek, issueCountByWeek, userCountByWeek);

        LineChart cumulativeTimeline = createCumulativeTimelineChart(evalsByWeek, issueCountByWeek, userCountByWeek);

        // print results
        resp.setStatus(200);

        resp.getOutputStream().print("<html>" +
                                     "<head><title>FindBugs Cloud Stats</title></head>" +
                                     "<body>");
        showChartImg(resp, evalsOverTimeChart.toURLString());
        resp.getOutputStream().print("<br><br>");
        showChartImg(resp, cumulativeTimeline.toURLString());
        resp.getOutputStream().print("<br><br>");
        showChartImg(resp, evalsByUserChart.toURLString());
        resp.getOutputStream().print("<br><br>");
        showChartImg(resp, histogram.toURLString());
    }

    private BarChart buildEvaluatorsHistogram(Multimap<String, String> issuesByUser) {
        Map<String, Integer> usersPerIssue = Maps.newHashMap();
        for (String email : issuesByUser.keySet()) {
            for (String hash : issuesByUser.get(email)) {
                increment(usersPerIssue, hash);
            }
        }

        // map from # of evaluators -> # of issues with that many evaluators
        Map<Integer, Integer> issuesPerEvaluatorCount = Maps.newHashMap();
        for (String issue : usersPerIssue.keySet()) {
            increment(issuesPerEvaluatorCount, usersPerIssue.get(issue));
        }

        List<Double> histogramData = Lists.newArrayList();
        int maxEvaluators = Collections.max(issuesPerEvaluatorCount.keySet());
        int maxIssues = Collections.max(issuesPerEvaluatorCount.values());
        List<String> barLabels = Lists.newArrayList();
        for (int evaluators = 1; evaluators <= maxEvaluators; evaluators++) {
            Integer issuesWithThisManyEvaluators = issuesPerEvaluatorCount.get(evaluators);
            if (issuesWithThisManyEvaluators == null)
                issuesWithThisManyEvaluators = 0;
            histogramData.add(issuesWithThisManyEvaluators * 100.0 / maxIssues);
            barLabels.add(Integer.toString(evaluators));
        }
        BarChart histogram = GCharts.newBarChart(Plots.newBarChartPlot(Data.newData(histogramData)));
        histogram.setSize(400, 500);
        histogram.setBarWidth(BarChart.AUTO_RESIZE);
        histogram.setTitle("Histogram: Evaluators Per Issue");
        histogram.setDataEncoding(DataEncoding.TEXT);
        histogram.addXAxisLabels(AxisLabelsFactory.newAxisLabels(barLabels));
        histogram.addYAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, maxIssues));
        AxisLabels bigLabel = AxisLabelsFactory.newAxisLabels("No. of evaluators", 50);
        bigLabel.setAxisStyle(AxisStyle.newAxisStyle(Color.BLACK, 12, AxisTextAlignment.CENTER));
        histogram.addXAxisLabels(bigLabel);
        return histogram;
    }

    private <E> void increment(Map<E, Integer> map, E key) {
        Integer oldCount = map.get(key);
        if (oldCount == null) oldCount = 0;
        map.put(key, oldCount + 1);
    }

    private LineChart createTimelineChart(Map<Long, Integer> evalsByWeek, Map<Long, Integer> issueCountByWeek,
                                          Map<Long, Integer> userCountByWeek) {
        int maxEvalsPerWeek = Collections.max(evalsByWeek.values());

        List<Double> evalsData = new ArrayList<Double>();
        List<Double> userCountData = new ArrayList<Double>();
        List<Double> newIssuesData = Lists.newArrayList();
        List<String> timelineLabels = Lists.newArrayList();
        int issuesCount = 0;
        int userCount = 0;
        for (Calendar cal : iterateByWeek(evalsByWeek.keySet())) {
            Integer evalsThisWeek = evalsByWeek.get(cal.getTimeInMillis());
            Integer issuesThisWeek = issueCountByWeek.get(cal.getTimeInMillis());
            int newIssuesThisWeek;
            if (issuesThisWeek != null) {
                newIssuesThisWeek = (issuesThisWeek - issuesCount);
                issuesCount = issuesThisWeek;
            } else {
                newIssuesThisWeek = 0;
            }
            Integer usersThisWeek = userCountByWeek.get(cal.getTimeInMillis());
            int newUsersThisWeek;
            if (usersThisWeek != null) {
                newUsersThisWeek = (usersThisWeek - userCount);
                userCount = usersThisWeek;
            } else {
                newUsersThisWeek = 0;
            }

            evalsThisWeek = evalsThisWeek == null ? 0 : evalsThisWeek;
            evalsData.add(evalsThisWeek * 100.0 / maxEvalsPerWeek);
            userCountData.add(newUsersThisWeek * 100.0 / maxEvalsPerWeek);
            newIssuesData.add(newIssuesThisWeek * 100.0 / maxEvalsPerWeek);
            timelineLabels.add(DATE_FORMAT.format(new Date(cal.getTimeInMillis())));
        }

        Line evalsLine = Plots.newLine(Data.newData(evalsData), Color.LIGHTPINK, "New evals for existing issues");
        evalsLine.setFillAreaColor(Color.LIGHTPINK);

        Line issuesLine = Plots.newLine(Data.newData(newIssuesData), Color.ORCHID, "New issues evaluated");
        issuesLine.setFillAreaColor(Color.ORCHID);

        Line usersLine = Plots.newLine(Data.newData(userCountData), Color.LIGHTSTEELBLUE, "New users");

        LineChart chart = GCharts.newLineChart(evalsLine, issuesLine, usersLine);
        chart.setTitle("New Evaluations, Issues, & Users Over Time");
        chart.setDataEncoding(DataEncoding.TEXT);
        chart.setSize(850, 350);

        chart.setGrid(100, 10 / (maxEvalsPerWeek / 100.0), 4, 1);

        chart.addXAxisLabels(AxisLabelsFactory.newAxisLabels(timelineLabels));
        chart.addYAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, maxEvalsPerWeek));
        return chart;
    }

    private LineChart createCumulativeTimelineChart(Map<Long, Integer> evalsByWeek, Map<Long, Integer> issueCountByWeek,
                                                    Map<Long, Integer> userCountByWeek) {
        int totalEvals = 0;
        for (Integer val : evalsByWeek.values()) {
            totalEvals += val;
        }
        int totalUsers = Collections.max(userCountByWeek.values());

        List<Double> issuesData = Lists.newArrayList();
        List<Double> evalsData = Lists.newArrayList();
        List<Double> usersData = Lists.newArrayList();
        List<String> labels = Lists.newArrayList();
        int issuesCount = 0;
        int userCount = 0;
        int evalCount = 0;
        for (Calendar cal : iterateByWeek(evalsByWeek.keySet())) {
            long time = cal.getTimeInMillis();
            Integer issuesThisWeek = issueCountByWeek.get(time);
            if (issuesThisWeek != null)
                issuesCount = issuesThisWeek;
            Integer usersThisWeek = userCountByWeek.get(time);
            if (usersThisWeek != null)
                userCount = usersThisWeek;
            Integer evalsThisWeek = evalsByWeek.get(time);
            if (evalsThisWeek != null)
                evalCount += evalsThisWeek;
            labels.add(DATE_FORMAT.format(new Date(time)));
            issuesData.add(issuesCount * 100.0 / totalEvals);
            usersData.add(userCount * 100.0 / totalUsers);
            evalsData.add(evalCount * 100.0 / totalEvals);
        }

        Line evalsLine = Plots.newLine(Data.newData(evalsData), Color.LIGHTPINK, "Total Evaluations");
        evalsLine.setFillAreaColor(Color.LIGHTPINK);

        Line issuesLine = Plots.newLine(Data.newData(issuesData), Color.ORCHID, "Total Issues Evaluated");
        issuesLine.setFillAreaColor(Color.ORCHID);

        Line usersLine = Plots.newLine(Data.newData(usersData), Color.LIGHTSTEELBLUE, "Total Users");

        LineChart chart = GCharts.newLineChart(evalsLine, issuesLine, usersLine);
        chart.setTitle("Growth Over Time");
        chart.setDataEncoding(DataEncoding.TEXT);
        chart.setSize(800, 350);

        chart.setGrid(100, 100 / (totalEvals / 100.0), 8, 2);

        chart.addXAxisLabels(AxisLabelsFactory.newAxisLabels(labels));
        AxisLabels leftLabels = AxisLabelsFactory.newNumericRangeAxisLabels(0, totalEvals);
        leftLabels.setAxisStyle(AxisStyle.newAxisStyle(Color.DARKORCHID, 10, AxisTextAlignment.RIGHT));
        chart.addYAxisLabels(leftLabels);

        AxisLabels rightLabels = AxisLabelsFactory.newNumericRangeAxisLabels(0, totalUsers);
        rightLabels.setAxisStyle(AxisStyle.newAxisStyle(Color.STEELBLUE, 10, AxisTextAlignment.LEFT));
        chart.addRightAxisLabels(rightLabels);
        return chart;
    }

    private Iterable<Calendar> iterateByWeek(Set<Long> unixtimes) {
        final long first = Collections.min(unixtimes);
        final long last = Collections.max(unixtimes);
        return new Iterable<Calendar>() {
            public Iterator<Calendar> iterator() {
                return new Iterator<Calendar>() {
                    private Calendar cal = Calendar.getInstance();
                    {
                        cal.setTimeInMillis(first);
                    }

                    public boolean hasNext() {
                        return cal.getTimeInMillis() <= last;
                    }

                    public Calendar next() {
                        if (!hasNext())
                            throw new NoSuchElementException();
                        Calendar toReturn = (Calendar) cal.clone();
                        cal.add(Calendar.DAY_OF_MONTH, 7);
                        return toReturn;
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
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
        BarChart chart = GCharts.newBarChart(Plots.newBarChartPlot(Data.newData(issues), Color.DARKORCHID, "Initial evaluation"),
                                             Plots.newBarChartPlot(Data.newData(totals), Color.ORCHID, "Updated evaluation"));

        chart.setTitle("Evaluations Per Human");
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
