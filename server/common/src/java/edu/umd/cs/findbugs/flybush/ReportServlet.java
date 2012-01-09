package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Supplier;
import com.google.common.collect.HashMultimap;
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
import com.googlecode.charts4j.GChart;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.Line;
import com.googlecode.charts4j.LineChart;
import com.googlecode.charts4j.Plots;
import org.apache.commons.lang.StringEscapeUtils;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

public class ReportServlet extends AbstractFlybushServlet {
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
            if (uri.equals("/stats")) {
                showStats(req, resp, pm);

            } else {
                show404(resp);
            }
        } finally {
            pm.close();
        }
    }

    private void showStats(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm) throws IOException {
        if (req.getParameter("user") != null) {
            showUserStats(req, resp, pm, req.getParameter("user"));
        } else if (req.getParameter("package") != null) {
            showPackageStats(req, resp, pm, req.getParameter("package"));
        } else {
            showSummaryStats(req, resp, pm);
        }
    }

    @SuppressWarnings({ "unchecked" })
    private void showPackageStats(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm, String desiredPackage)
            throws IOException {
        if (desiredPackage.equals("*"))
            desiredPackage = "";

        Query query = pm.newQuery("select from " + persistenceHelper.getDbEvaluationClassname()
                + " where packages.contains(:pkg)" + " order by when asc");
        List<DbEvaluation> evals = (List<DbEvaluation>) query.execute(desiredPackage);

        Map<Long, Integer> evalsPerWeek = Maps.newHashMap();
        Map<Long, Integer> newIssuesPerWeek = Maps.newHashMap();
        Multimap<String, String> issuesByPkg = HashMultimap.create();
        Set<String> seenIssues = Sets.newHashSet();
        Map<String, Integer> evalsByPkg = Maps.newHashMap();
        List<String> lines = Lists.newArrayList();
        for (DbEvaluation eval : evals) {
            String epkg = getPackageName(eval.getPrimaryClass());
            if (epkg == null)
                continue;
            if (!isPackageOrSubPackage(desiredPackage, epkg)) {
                continue;
            }
            long beginningOfWeek = getBeginningOfWeekInMillis(eval.getWhen());
           
            String issueHash = eval.getIssue().getHash();
            issuesByPkg.put(epkg, issueHash);
            increment(evalsByPkg, epkg);
            increment(evalsPerWeek, beginningOfWeek);
            if (seenIssues.add(issueHash))
                increment(newIssuesPerWeek, beginningOfWeek);
            lines.add(createEvalsTableEntry(req, eval));
        }
        query.closeAll();

        LineChart timelineChart = null;
        BarChart subpkgChart = null;
        if (evalsPerWeek.size() != 0) {
            timelineChart = buildEvaluationTimeline("Reviews & Issues - " + desiredPackage + " (recursive)", evalsPerWeek,
                    newIssuesPerWeek);
            if (evalsByPkg.size() > 1)
                subpkgChart = buildByPkgChart(issuesByPkg, evalsByPkg);
        }

        resp.setStatus(200);
        ServletOutputStream page = resp.getOutputStream();
        page.println("<html>\n" + "<head><title>" + escapeHtml(desiredPackage) + " - " + getCloudName() + " Stats</title></head>\n"
                + "<body>\n" + backButton(req));

        printPackageForm(req, resp, desiredPackage);

        printPackageTree(req, resp, desiredPackage, evalsByPkg);

        page.println("<br><br>");

        if (timelineChart == null)
            page.print("No reviews for classes in " + escapeHtml(desiredPackage));
        else
            showChartImg(resp, timelineChart);
        if (subpkgChart != null)
            showChartImg(resp, subpkgChart);

        printEvalsTable(resp, lines);
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

    private void printPackageTree(HttpServletRequest req, HttpServletResponse resp, String desiredPackage,
            Map<String, Integer> evalsByPkg) throws IOException {
        ServletOutputStream page = resp.getOutputStream();
        boolean isDefaultPackage = desiredPackage.equals("");
        page.println("<ul>");
        if (desiredPackage.contains("."))
            page.println("<li> " + linkToPkg(req, desiredPackage.substring(0, desiredPackage.lastIndexOf('.'))));
        else if (isDefaultPackage)
            page.println("<li>");
        else
            page.println("<li> <a href=\"" + req.getRequestURI() + "?package=*\">&lt;default package&gt;</a>");
        page.println("<ul>");
        page.println("<li> " + (isDefaultPackage ? "&lt;default package&gt;" : escapeHtml(desiredPackage)) + " ("
                + sum(evalsByPkg.values()) + ")");
        page.println("<ul>");
        Set<String> alreadyPrinted = Sets.newHashSet();
        
        for (String pkg : Sets.newTreeSet(evalsByPkg.keySet())) {
            if (pkg.equals(desiredPackage))
                continue;
            String remainder = isDefaultPackage ? pkg : pkg.substring(desiredPackage.length() + 1);
            int dot = remainder.indexOf('.');
            String subpkg;
            if (dot != -1) {
                subpkg = remainder.substring(0, dot);
            } else {
                subpkg = remainder;
            }
            String pkgToPrint = isDefaultPackage ? subpkg : desiredPackage + "." + subpkg;
            if (!alreadyPrinted.add(pkgToPrint))
                continue;
            page.println("<li> " + linkToPkg(req, pkgToPrint) + " (" + getEvalCountForTree(evalsByPkg, pkgToPrint) + ")</li>");
        }
        page.println("</li>");
        page.println("</li>");
        page.println("</ul>");
    }

    private int sum(Iterable<Integer> values) {
        int x = 0;
        for (Integer value : values) {
            x += value;
        }
        return x;
    }

    private String linkToPkg(HttpServletRequest req, String pkg) {
        String escaped = escapeHtml(pkg);
        try {
            return "<a href=\"" + req.getRequestURI() + "?package=" + URLEncoder.encode(escaped, "UTF-8") + "\">" + escaped
                    + "</a>";
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE, pkg, e);
            return "(ERROR)";
        }
    }

    private int getEvalCountForTree(Map<String, Integer> evalsByPkg, String rootPackage) {
        int x = 0;
        for (Entry<String, Integer> entry : evalsByPkg.entrySet()) {
            if (isPackageOrSubPackage(rootPackage, entry.getKey()))
                x += entry.getValue();
        }
        return x;
    }

    @SuppressWarnings({ "SimplifiableIfStatement" })
    private static boolean isPackageOrSubPackage(String possibleParent, String possibleChild) {
        if (possibleParent.equals(""))
            return true;
        return possibleChild.equals(possibleParent) || possibleChild.startsWith(possibleParent + ".");
    }

    @SuppressWarnings({ "unchecked" })
    private void showUserStats(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm, String email)
            throws IOException {
        Query query = pm.newQuery("select from " + persistenceHelper.getDbEvaluationClassname()
                + " where email == :email order by when asc");
        List<DbEvaluation> evals = (List<DbEvaluation>) query.execute(email);
        if (evals.isEmpty()) {
            setResponse(resp, 404, "No such user");
        }

        Map<Long, Integer> evalsPerWeek = Maps.newHashMap();
        Set<String> seenIssues = Sets.newHashSet();
        Map<Long, Integer> newIssuesByWeek = Maps.newHashMap();
        List<String> table = Lists.newArrayList();
        for (DbEvaluation eval : evals) {
            long beginningOfWeek = getBeginningOfWeekInMillis(eval.getWhen());
            increment(evalsPerWeek, beginningOfWeek);
            if (seenIssues.add(eval.getIssue().getHash()))
                increment(newIssuesByWeek, beginningOfWeek);
            table.add(createEvalsTableEntry(req, eval));
        }
        query.closeAll();

        LineChart chart = null;
        if (!evalsPerWeek.isEmpty())
            chart = buildEvaluationTimeline("Reviews over Time - " + email, evalsPerWeek, newIssuesByWeek);

        resp.setStatus(200);
        ServletOutputStream page = resp.getOutputStream();
        page.println("<html>\n" + "<head><title>" + escapeHtml(email) + " - " + getCloudName() + " Stats</title></head>\n" + "<body>\n"
                + backButton(req));

        Set<String> users = getAllUserEmails(pm);
        printUserStatsSelector(req, resp, users, email);

        if (chart == null) {
            page.println("Oops! No reviews uploaded by " + escapeHtml(email));
            return;
        }
        showChartImg(resp, chart);

        printEvalsTable(resp, table);
    }

    /**
     * @param table
     *            should be sorted in ascending order
     */
    private static void printEvalsTable(HttpServletResponse resp, List<String> table) throws IOException {
        ServletOutputStream page = resp.getOutputStream();
        page.println("<br><br>Review history:");
        page.println("<div style='overflow:auto;height:400px;border:2px solid black'>\n"
                + "<table cellspacing=0 border=1 cellpadding=6>\n"
                + "<tr><th>Date (UTC)</th><th>User</th><th>Class</th><th>Designation</th><th>Comment</th></tr>");
        Collections.reverse(table);
        int odd = 0;
        for (String line : table) {
            page.println("<tr style=background-color:" + ((++odd % 2 == 0) ? "#ffddee" : "white") + ";vertical-align:top>" + line
                    + "</tr>");
        }
        page.println("</table>\n" + "</div>");
    }

    private static String createEvalsTableEntry(HttpServletRequest req, DbEvaluation eval) {
        return String.format("<td>%s</td>" + "<td><a href='%s'>%s</a></td>" + "<td>%s</td>" + "<td>%s</td>" + "<td>%s</td>",
                DATE_TIME_FORMAT().format(new Date(eval.getWhen())).replaceAll(" UTC", ""), req.getRequestURI() + "?user="
                        + escapeHtml(eval.getEmail()), escapeHtml(eval.getEmail()),
                /* eval.getIssue().getBugPattern(), */
                printPackageLinks(req, eval.getPrimaryClass()), escapeHtml(eval.getDesignation()), escapeHtml(eval.getComment()));
    }

    @SuppressWarnings({ "StringConcatenationInsideStringBufferAppend" })
    private static String printPackageLinks(HttpServletRequest req, String cls) {
        StringBuilder str = new StringBuilder();
        int lastdot;
        int dot = -1;
        while (true) {
            lastdot = dot;
            dot = cls.indexOf('.', dot + 1);
            if (dot == -1) {
                str.append(cls.substring(lastdot));
                break;
            }
            String sofar = cls.substring(0, dot);
            if (lastdot != -1)
                str.append(".");
            str.append("<a href=\"" + req.getRequestURI() + "?package=" + StringEscapeUtils.escapeHtml(sofar) + "\">");
            str.append(cls.substring(lastdot + 1, dot));
            str.append("</a>");
        }
        return str.toString();
    }

    @SuppressWarnings({ "unchecked" })
    private Set<String> getAllUserEmails(PersistenceManager pm) {
        Query userQuery = pm.newQuery("select from " + persistenceHelper.getDbUserClassname());
        List<DbUser> userObjs = (List<DbUser>) userQuery.execute();
        Set<String> users = Sets.newHashSet();
        for (DbUser userObj : userObjs) {
            users.add(userObj.getEmail());
        }
        userQuery.closeAll();
        return users;
    }

    private LineChart buildEvaluationTimeline(String title, Map<Long, Integer> evalsPerWeek, Map<Long, Integer> newIssuesByWeek) {
        int maxEvalsPerWeek = Collections.max(evalsPerWeek.values());
        List<Double> evalsData = Lists.newArrayList();
        List<String> labels = Lists.newArrayList();
        List<Double> newIssuesData = Lists.newArrayList();
        boolean first = true;
        for (Calendar cal : iterateByWeek(evalsPerWeek.keySet())) {
            Integer evalsThisWeek = evalsPerWeek.get(cal.getTimeInMillis());
            if (evalsThisWeek == null)
                evalsThisWeek = 0;
            Integer newIssuesThisWeek = newIssuesByWeek.get(cal.getTimeInMillis());
            if (newIssuesThisWeek == null)
                newIssuesThisWeek = 0;
            evalsData.add(evalsThisWeek * 100.0 / maxEvalsPerWeek);
            newIssuesData.add(newIssuesThisWeek * 100.0 / maxEvalsPerWeek);
            labels.add(formatDate(cal, first));
            first = false;
        }

        Line evalsLine = Plots.newLine(Data.newData(evalsData), Color.LIGHTPINK, "Updated reviews");
        evalsLine.setFillAreaColor(Color.LIGHTPINK);
        Line issuesLine = Plots.newLine(Data.newData(newIssuesData), Color.ORCHID, "Initial reviews");
        issuesLine.setFillAreaColor(Color.ORCHID);
        LineChart chart = GCharts.newLineChart(evalsLine, issuesLine);
        chart.addXAxisLabels(AxisLabelsFactory.newAxisLabels(labels));
        chart.addYAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, maxEvalsPerWeek));
        chart.setTitle(title);
        chart.setDataEncoding(DataEncoding.TEXT);
        chart.setSize(800, 350);
        return chart;
    }

    @SuppressWarnings({ "unchecked" })
    private void showSummaryStats(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm) throws IOException {
        Query query = pm.newQuery("select from " + persistenceHelper.getDbEvaluationClassname() + " order by when ascending");
        List<DbEvaluation> evals = (List<DbEvaluation>) query.execute();
        Map<String, Integer> totalCountByUser = Maps.newHashMap();
        Map<String, Integer> issueCountByUser = Maps.newHashMap();
        Multimap<String, String> issuesByUser = Multimaps.newSetMultimap(Maps.<String, Collection<String>>newHashMap(),
                new Supplier<Set<String>>() {
                    public Set<String> get() {
                        return Sets.newHashSet();
                    }
                });
        Map<Long, Integer> evalsByWeek = Maps.newHashMap();
        Map<Long, Integer> issueCountByWeek = Maps.newHashMap();
        Map<Long, Integer> userCountByWeek = Maps.newHashMap();
        Map<String, Integer> evalCountByPkg = Maps.newHashMap();
        Multimap<String, String> issuesByPkg = HashMultimap.create();
        Set<String> seenIssues = Sets.newHashSet();
        Set<String> seenUsers = Sets.newHashSet();

        for (DbEvaluation eval : evals) {
            String email = eval.getEmail();
            if (email == null)
                continue;

            long beginningOfWeek = getBeginningOfWeekInMillis(eval.getWhen());
            String issueHash = eval.getIssue().getHash();
            issuesByUser.put(email, issueHash);
            increment(totalCountByUser, email);
            issueCountByUser.put(email, issuesByUser.get(email).size());

            String pkg = getPackageName(eval.getPrimaryClass());
            if (pkg != null) {
                increment(evalCountByPkg, pkg);
                issuesByPkg.put(pkg, issueHash);
            }

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

        BarChart evalsByPkgChart = buildByPkgChart(issuesByPkg, evalCountByPkg);

        LineChart evalsOverTimeChart = createTimelineChart(evalsByWeek, issueCountByWeek, userCountByWeek);

        LineChart cumulativeTimeline = createCumulativeTimelineChart(evalsByWeek, issueCountByWeek, userCountByWeek);

        // print results
        resp.setStatus(200);

        ServletOutputStream page = resp.getOutputStream();
        page.println("<html>" +
                "<head>" +
                "<title>" + getCloudName() + " Stats</title>" +
                "<script type='application/javascript'>\n" +
                "    // Send the POST when the page is loaded,\n" +
                "    // which will replace this whole page with the retrieved chart.\n" +
                "    function loadGraph() {\n" +
                "      for (var i = 1; i < 20; i++) {" +
                "        var frm = document.getElementById('post_form_' + i);\n" +
                "        if (!frm) break;\n" +
                "        frm.submit();\n" +
                "      }\n" +
                "    }\n" +
                "  </script>" +
                "</head>"
                + "<body onload='loadGraph()'>");
        showChartImg(resp, evalsOverTimeChart);
        page.println("<br><br>");
        showChartImg(resp, cumulativeTimeline);
        page.println("<br><br>");

        printUserStatsSelector(req, resp, seenUsers, null);
        printPackageForm(req, resp, "");

        showChartImg(resp, evalsByPkgChart);
        page.println("<br><br>");
        showChartImg(resp, evalsByUserChart);
        page.println("<br><br>");
        showChartImg(resp, histogram);
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
    protected void showChartImg(HttpServletResponse resp, GChart chart) throws IOException {
        Matcher m = Pattern.compile("(\\d+)x(\\d+)").matcher(chart.getParameters().get("chs"));
        String width, height;
        if (m.find()) {
            width = m.group(1);
            height = m.group(2);
        } else {
            width = "300";
            height = "200";
        }

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
                "width=\"" + width + "\" height=\"" + height + "\"></iframe>");
    }

    @Override
    protected void handlePost(PersistenceManager pm, HttpServletRequest req, HttpServletResponse resp, String uri)
            throws IOException {
    }
}
