package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.googlecode.charts4j.AxisLabels;
import com.googlecode.charts4j.AxisLabelsFactory;
import com.googlecode.charts4j.AxisStyle;
import com.googlecode.charts4j.AxisTextAlignment;
import com.googlecode.charts4j.BarChart;
import com.googlecode.charts4j.BarChartPlot;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.Data;
import com.googlecode.charts4j.DataEncoding;
import com.googlecode.charts4j.GChart;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.Line;
import com.googlecode.charts4j.LineChart;
import com.googlecode.charts4j.Plots;
import org.apache.commons.lang.StringEscapeUtils;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

public class UsageReportServlet extends AbstractFlybushServlet {
    private int form_id = 0;

    private static  DateFormat DATE_TIME_FORMAT() {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
    }

    private static  SimpleDateFormat DF_M_D_Y() {
        return new SimpleDateFormat("M/d/yy");
    }

    private static  SimpleDateFormat DF_M_D() {
        return new SimpleDateFormat("M/d");
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String uri = req.getRequestURI();
        PersistenceManager pm = getPersistenceManager();
        try {
            if (uri.equals("/usage")) {
                showStats(req, resp, pm);

            } else {
                show404(resp);
            }
        } finally {
            pm.close();
        }
    }
    
    /*
    Important stats:
    - per day:
      - users by version, stacked
    - past month:
      - users by country
      - users by os
      - users by java version
      - users by language
    - plugin usage
      - drill-down: plugin versions
     */

    private void showStats(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm) throws IOException {
        LOGGER.warning("About to execute query");
        Query query = pm.newQuery("select from " + persistenceHelper.getDbUsageEntryClassname()
                + " where date > :date order by date ascending");
        query.addExtension("javax.persistence.query.chunkSize", 200);
        @SuppressWarnings("unchecked")
        List<DbUsageEntry> entries = (List<DbUsageEntry>) query.execute(oneWeekAgo());

        Multimap<String, DbUsageEntry> entriesByUuid = newHashSetMultiMap();
        Multimap<String, String> pluginsByUuid = newHashSetMultiMap();
        Multimap<String, String> uuidsByPlugin = newHashSetMultiMap();
        Multimap<String, String> uuidsByVersion = newHashSetMultiMap();
        Multimap<Long, String> usersByWeek = newHashSetMultiMap();
        Multimap<Long, String> usersByDay = newHashSetMultiMap();
        Multimap<Long, String> ipsByDay = newHashSetMultiMap();
        LOGGER.warning("Total entries?");
        int size = entries.size();
        LOGGER.warning("Total entries: " + size);
        int count = 0;
        for (DbUsageEntry entry : entries) {
            if (++count % 1000 == 0) {
                LOGGER.warning("Processed " + count + " of " + size + " - " + String.format("%.2f", count*100.0/size) + "%");
            }
            String uuid = entry.getUuid();
            usersByWeek.put(weekStart(entry.getDate()).getTime(), uuid);
            usersByDay.put(dayStart(entry.getDate()).getTime(), uuid);
            ipsByDay.put(dayStart(entry.getDate()).getTime(), entry.getIpAddress());
            entriesByUuid.put(uuid, entry);
            String plugin = entry.getPlugin();
            boolean corePlugin = "edu.umd.cs.findbugs.plugins.core".equals(plugin);
            if (!corePlugin && !"".equals(plugin)) {
                pluginsByUuid.put(uuid, entry.getPlugin());
                uuidsByPlugin.put(entry.getPlugin(), uuid);
            }
            uuidsByVersion.put(entry.getVersion(), uuid);
        }
        
        
        query.closeAll();

        // build charts
//        BarChart histogram = buildEvaluatorsHistogram(issuesByUser);
//
//        BarChart evalsByUserChart = buildByUserChart(totalCountByUser, issueCountByUser);
//
//        BarChart evalsByPkgChart = buildByPkgChart(issuesByPkg, evalCountByPkg);

        LineChart usersByVersionPerDay = createTimelineChart2(usersByDay, "Unique Users");
        LineChart ipsByVersionPerDay = createTimelineChart2(ipsByDay, "Unique IP Addresses");
        BarChart pluginsChart = makeBarChart(uuidsByPlugin, "Unique Plugin Users", 600, 200);
        BarChart versionsChart = makeBarChart(uuidsByVersion, "FindBugs Versions", 600, 400);

//        LineChart cumulativeTimeline = createCumulativeTimelineChart(evalsByWeek, issueCountByWeek, userCountByWeek);

        // print results
        resp.setStatus(200);

        ServletOutputStream page = printHtmlHeader(resp, getCloudName() + " Stats");
//        showChartImg(resp, evalsOverTimeChart);
//        page.println("<br><br>");
//        showChartImg(resp, cumulativeTimeline);
//        page.println("<br><br>");

//        printUserStatsSelector(req, resp, seenUsers, null);
//        printPackageForm(req, resp, "");

//        showChartImg(resp, evalsByPkgChart);
//        page.println("<br><br>");
//        showChartImg(resp, evalsByUserChart);
        showChartImg(resp, usersByVersionPerDay, true);
        page.println("<br><br>");
        showChartImg(resp, ipsByVersionPerDay, true);
        page.println("<br><br>");
        showChartImg(resp, versionsChart, false);
        page.println("<br><br>");
        showChartImg(resp, pluginsChart, false);
        page.println("<br><br>");
    }

    private BarChart makeBarChart(Multimap<String, String> uuidsByPlugin, String title, int width, int height) {
        double max = 0;
        for (Collection<String> strings : uuidsByPlugin.asMap().values()) {
            max = Math.max(max, strings.size());
        }
        List<Double> data = Lists.newArrayList();
        List<String> labels = Lists.newArrayList();
        for (Entry<String, Collection<String>> entry : uuidsByPlugin.asMap().entrySet()) {
            int val = entry.getValue().size();
            data.add(val/max*100);
            labels.add(entry.getKey());
        }
        Collections.reverse(labels);
        BarChartPlot plot = Plots.newBarChartPlot(Data.newData(data));
        BarChart chart = GCharts.newBarChart(plot);
        chart.setHorizontal(true);
        chart.addYAxisLabels(AxisLabelsFactory.newAxisLabels(labels));
        chart.addXAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, max, 50));
//        chart.setLegendMargins(100, 100);
//        chart.setMargins(100, 0, 100, 0);
        chart.setSize(width, height);
        chart.setTitle(title);
        return chart;
    }

    private static String shortenFQN(String fqn) {
        StringBuilder str = new StringBuilder();
        for (Iterator<String> iterator = Splitter.on(".").split(fqn).iterator(); iterator.hasNext(); ) {
            String part = iterator.next();
            if (iterator.hasNext()) {
                str.append(part.substring(0,1)).append('.');
            } else {
                str.append(part);
            }
        }

        return str.toString();
    }

    private Date weekStart(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date dayStart(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static <K,V> SetMultimap<K, V> newHashSetMultiMap() {
        return Multimaps.newSetMultimap(Maps.<K, Collection<V>>newHashMap(),
                new Supplier<Set<V>>() {
                    public Set<V> get() {
                        return Sets.newHashSet();
                    }
                });
    }

    private static Date oneWeekAgo() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -2);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }


    private String backButton(HttpServletRequest req) {
        return "<a href=\"" + req.getRequestURI() + "\">&lt;&lt; Back</a>";
    }

    private void printPackageForm(HttpServletRequest req, HttpServletResponse resp, String desiredPackage) throws IOException {
        resp.getOutputStream().println(
                "<form action=\"" + req.getRequestURI() + "\" method=get>\n"
                        + "Package stats:  <input type=text name=package size=30 value=\"" + escapeHtml(desiredPackage) + "\">\n"
                        + "<input type=submit value=Go>\n" + "</form>");
    }


    private ServletOutputStream printHtmlHeader(HttpServletResponse resp, String title) throws IOException {
        ServletOutputStream page = resp.getOutputStream();
        page.println("<html>" +
                "<head>" +
                "<title>" + title + "</title>" +
                "<script type='application/javascript'>\n" +
                "    // Send the POST when the page is loaded,\n" +
                "    // which will replace this whole page with the retrieved chart.\n" +
                "    function loadGraph() {\n" +
                "      for (var i = 1; i < 40; i++) {" +
                "        var frm = document.getElementById('post_form_' + i);\n" +
                "        if (frm) \n" +
                "          frm.submit();\n" +
                "      }\n" +
                "    }\n" +
                "  </script>" +
                "</head>"
                + "<body onload='loadGraph()'>");
        return page;
    }

    private String getPackageName(String className) {
        if (className == null) {
            return null;
        }
        int lastDot = className.lastIndexOf('.');
        if (lastDot == -1) {
            return null;
        }
        return className.substring(0, lastDot);
    }

    private void printUserStatsSelector(HttpServletRequest req, HttpServletResponse resp, Collection<String> seenUsers,
            String selectedEmail) throws IOException {
        ServletOutputStream page = resp.getOutputStream();
        page.println("User stats:" + "<form action=\"" + req.getRequestURI() + "\" method=get>\n" + "<select name=user>\n"
                + "<option value=\"\"></option>");
        List<String> seenUsersList = Lists.newArrayList(seenUsers);
        Collections.sort(seenUsersList);
        for (String email : seenUsersList) {
            String escaped = escapeHtml(email);
            page.println(String.format("<option value=\"%s\"%s>%s</option>", escaped, email.equals(selectedEmail) ? " selected"
                    : "", escaped));
        }
        page.println("</select>\n" + "<input type=submit value=Go>\n" + "</form>");
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
        histogram.setTitle("Histogram: Reviewers Per Issue");
        histogram.setDataEncoding(DataEncoding.TEXT);
        histogram.addXAxisLabels(AxisLabelsFactory.newAxisLabels(barLabels));
        histogram.addYAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, maxIssues));
        AxisLabels bigLabel = AxisLabelsFactory.newAxisLabels("No. of reviewers", 50);
        bigLabel.setAxisStyle(AxisStyle.newAxisStyle(Color.BLACK, 12, AxisTextAlignment.CENTER));
        histogram.addXAxisLabels(bigLabel);
        return histogram;
    }

    private <E> void increment(Map<E, Integer> map, E key) {
        Integer oldCount = map.get(key);
        if (oldCount == null)
            oldCount = 0;
        map.put(key, oldCount + 1);
    }

    //TODO: should we just use Google Analytics??
    private LineChart createTimelineChart2(Multimap<Long, String> byDay, String title) {
        Map<Long, Integer> byDayCounts = Maps.newHashMap();
        for (Entry<Long, Collection<String>> entry : byDay.asMap().entrySet()) {
            byDayCounts.put(entry.getKey(), entry.getValue().size());
        }
        int maxPerDay = Collections.max(byDayCounts.values());

        List<Double> userCountData = new ArrayList<Double>();
        List<String> timelineLabels = Lists.newArrayList();
        boolean first = true;
        for (Calendar cal : iterateByDay(byDay.keySet())) {
            Integer usersThisWeek = byDayCounts.get(cal.getTimeInMillis());
            if (usersThisWeek == null) 
                usersThisWeek = 0;

            userCountData.add(usersThisWeek * 100.0 / maxPerDay);
            timelineLabels.add(formatDate(cal, first));
            first = false;
        }

        Line userCountLine = Plots.newLine(Data.newData(userCountData), Color.LIGHTPINK);
        userCountLine.setFillAreaColor(Color.LIGHTPINK);

        LineChart chart = GCharts.newLineChart(userCountLine);
        chart.setTitle(title);
        chart.setDataEncoding(DataEncoding.TEXT);
        chart.setSize(850, 350);

        chart.setGrid(100, 10 / (maxPerDay / 100.0), 4, 1);

        chart.addXAxisLabels(AxisLabelsFactory.newAxisLabels(timelineLabels));
        chart.addYAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, maxPerDay));
        return chart;
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
        boolean first = true;
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
            timelineLabels.add(formatDate(cal, first));
            first = false;
        }

        Line evalsLine = Plots.newLine(Data.newData(evalsData), Color.LIGHTPINK, "New evals for existing issues");
        evalsLine.setFillAreaColor(Color.LIGHTPINK);

        Line issuesLine = Plots.newLine(Data.newData(newIssuesData), Color.ORCHID, "New issues evaluated");
        issuesLine.setFillAreaColor(Color.ORCHID);

        Line usersLine = Plots.newLine(Data.newData(userCountData), Color.LIGHTSTEELBLUE, "New users");

        LineChart chart = GCharts.newLineChart(evalsLine, issuesLine, usersLine);
        chart.setTitle("New Reviews, Issues, & Users Over Time");
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
        boolean first = true;
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
            labels.add(formatDate(time, first));
            issuesData.add(issuesCount * 100.0 / totalEvals);
            usersData.add(userCount * 100.0 / totalUsers);
            evalsData.add(evalCount * 100.0 / totalEvals);
            first = false;
        }

        Line evalsLine = Plots.newLine(Data.newData(evalsData), Color.LIGHTPINK, "Total Reviews");
        evalsLine.setFillAreaColor(Color.LIGHTPINK);

        Line issuesLine = Plots.newLine(Data.newData(issuesData), Color.ORCHID, "Total Issues Reviewed");
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

    private String formatDate(Calendar cal, boolean firstDateSoFar) {
        if (firstDateSoFar || cal.get(Calendar.MONTH) == Calendar.JANUARY) {
            return DF_M_D_Y().format(new Date(cal.getTimeInMillis()));
        } else {
            return DF_M_D().format(new Date(cal.getTimeInMillis()));
        }
    }

    private String formatDate(long time, boolean firstDateSoFar) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        return formatDate(cal, firstDateSoFar);
    }

    private Iterable<Calendar> iterateByWeek(Set<Long> unixtimes) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(Collections.min(unixtimes));
        cal.add(Calendar.DAY_OF_MONTH, -7); // one week prior

        final long first = cal.getTimeInMillis();
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

    private Iterable<Calendar> iterateByDay(Set<Long> unixtimes) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(Collections.min(unixtimes));
//        cal.add(Calendar.DAY_OF_MONTH, -1);

        final long first = cal.getTimeInMillis();
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
                        cal.add(Calendar.DAY_OF_MONTH, 1);
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

    private BarChart buildByPkgChart(Multimap<String, String> issuesByPkg, Map<String, Integer> evalCountByPkg) {
        List<String> labels = Lists.newArrayList();
        List<Double> evalsData = Lists.newArrayList();
        List<Double> issuesData = Lists.newArrayList();
        int maxPerPkg = Collections.max(evalCountByPkg.values());
        int count = 0;
        List<Entry<String, Integer>> entries = sortEntriesByValue(evalCountByPkg.entrySet());
        Collections.reverse(entries);
        for (Entry<String, Integer> entry : entries) {
            labels.add(entry.getKey());
            evalsData.add(entry.getValue() * 100.0 / maxPerPkg);
            issuesData.add((double) issuesByPkg.get(entry.getKey()).size());
            if (count++ >= 19)
                break;
        }
        Collections.reverse(labels);
        BarChart chart = GCharts.newBarChart(Plots.newBarChartPlot(Data.newData(issuesData), Color.DARKORCHID, "Issues"),
                Plots.newBarChartPlot(Data.newData(evalsData), Color.ORCHID, "Evaluations"));

        chart.setTitle("Reviews Per Package");
        chart.setGrid(10.0 / (maxPerPkg / 100.0), 100, 4, 1);
        chart.setDataStacked(true);
        chart.addYAxisLabels(AxisLabelsFactory.newAxisLabels(labels));
        chart.addXAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, maxPerPkg, 10));
        chart.setBarWidth(BarChart.AUTO_RESIZE);
        chart.setSize(600, 500);
        chart.setHorizontal(true);
        chart.setDataEncoding(DataEncoding.TEXT);
        return chart;
    }

    private BarChart buildByUserChart(Map<String, Integer> totalCountByUser, Map<String, Integer> issueCountByUser) {
        List<String> labels = Lists.newArrayList();
        List<Double> totals = Lists.newArrayList();
        List<Double> issues = Lists.newArrayList();
        int max = Collections.max(totalCountByUser.values());
        for (Entry<String, Integer> entry : sortEntriesByValue(totalCountByUser.entrySet())) {
            String email = entry.getKey();
            int issueCount = issueCountByUser.get(email);
            int evalCount = entry.getValue();
            labels.add(email);
            totals.add((evalCount - issueCount) * 100.0 / max);
            issues.add(issueCount * 100.0 / max);
        }

        Collections.reverse(totals);
        Collections.reverse(issues);
        BarChart chart = GCharts.newBarChart(Plots.newBarChartPlot(Data.newData(issues), Color.DARKORCHID, "Initial review"),
                Plots.newBarChartPlot(Data.newData(totals), Color.ORCHID, "Updated review"));

        chart.setTitle("Reviews Per Human");
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

    private List<Entry<String, Integer>> sortEntriesByValue(Collection<Entry<String, Integer>> entries) {
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
    protected void showChartImg(HttpServletResponse resp, GChart chart, boolean iframe) throws IOException {
        Matcher m = Pattern.compile("(\\d+)x(\\d+)").matcher(chart.getParameters().get("chs"));
        String margins = chart.getParameters().get("chma");
        if (margins == null) margins = "";
        Matcher m2 = Pattern.compile("(\\d+),(\\d+),(\\d+),(\\d+)").matcher(margins);
        int actualwidth, actualheight;
        int width, height;
        if (m.find()) {
            width = Integer.parseInt(m.group(1));
            height = Integer.parseInt(m.group(2));
            actualwidth = width;
            actualheight = height;
            if (m2.find()) {
                actualwidth += Integer.parseInt(m2.group(1));
                actualwidth += Integer.parseInt(m2.group(3));
            }
        } else {
            width = 300;
            height = 200;
            actualwidth = width;
            actualheight = height;
        }
        
        if(iframe) {

            form_id++;
            Map<String, String> parameters = chart.getParameters();
            resp.getOutputStream().print(
                    "<form action='https://chart.googleapis.com/chart' method='POST' " +
                            "id='post_form_" + form_id + "'\n" +
                            "target='post_frame_" + form_id + "' " +
                            "onsubmit=\"this.action = 'https://chart.googleapis.com/chart?chid=' " +
                            "+ (new Date()).getMilliseconds(); return true;\">\n");
            for (Entry<String, String> entry : parameters.entrySet()) {
                resp.getOutputStream().println("<input type='hidden' name='" + entry.getKey()
                        + "' value='" + StringEscapeUtils.escapeHtml(URLDecoder.decode(entry.getValue(), "UTF-8")) + "'/>");
            }
            resp.getOutputStream().print("    </form>");
            resp.getOutputStream().print("<iframe name='post_frame_" + form_id + "' src=\"/empty.html\" " +
                    "width=\"" + actualwidth + "\" height=\"" + actualheight + "\"></iframe>");
        } else {
            resp.getOutputStream().print("<img src='" + chart.toURLForHTML() + "' style=width:" + width + "px;height:" + height + "px>");
        }
    }

    @Override
    protected void handlePost(PersistenceManager pm, HttpServletRequest req, HttpServletResponse resp, String uri)
            throws IOException {
    }
}
