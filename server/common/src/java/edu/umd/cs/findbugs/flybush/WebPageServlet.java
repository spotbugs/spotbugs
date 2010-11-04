package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringEscapeUtils;

public class WebPageServlet extends AbstractFlybushServlet {
    private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("MMM d, ''yy 'at' h:mm aa z");

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getRequestURI().startsWith("/issues/")) {
            handleIssueRequest(getPersistenceManager(), req, resp, req.getRequestURI());
        } else {
            show404(resp);
        }

    }

    @SuppressWarnings({"unchecked"})
    private void handleIssueRequest(PersistenceManager pm, HttpServletRequest req, HttpServletResponse resp, String uri)
            throws IOException {
        if (req.getParameter("embed") == null) {
            show404(resp);
            return;
        }
        Pattern p = Pattern.compile("/issues/([^/?]+)");
        Matcher m = p.matcher(uri);
        if (!m.matches()) {
            show404(resp);
            return;
        }
        String hash = m.group(1);
        Map<String,DbIssue> map = persistenceHelper.findIssues(pm, Lists.<String>newArrayList(hash));
        DbIssue issue = map.get(hash);
        if (issue == null) {
            printHtmlPreface(resp);
            resp.getWriter().println("<p>This issue has not been submitted to the " + getCloudName() + "</p>");
            LOGGER.info("Not in cloud");
            return;
        }

        List<DbEvaluation> list = Lists.newArrayList(sortAndFilterEvaluations(issue.getEvaluations()));
        LOGGER.info("Issue " + issue.getPrimaryClass() + " - " + list.size() + " comments");
        if (list.isEmpty()) {
            printHtmlPreface(resp);
            resp.getWriter().println("<p>No comments have been submitted for this issue</p>");
            return;
        }
        Collections.reverse(list);

        PrintWriter out = resp.getWriter();
        printHtmlPreface(resp);
        out.println("<table border=0 class=popup-evals cellspacing=15>");
        for (DbEvaluation evaluation : list) {
            out.println("<tr>");
            out.println("<td>" + StringEscapeUtils.escapeHtml(evaluation.getEmail())
                    + "<br><span class=timestamp>" + TIMESTAMP_FORMAT.format(new Date(evaluation.getWhen())) + " </span></td>");

            out.println("<td><strong>" + StringEscapeUtils.escapeHtml(evaluation.getDesignation()) + "</strong>" +
                    " &mdash; " + StringEscapeUtils.escapeHtml(evaluation.getComment()) + "</td>");
            out.println("</tr>");
        }
        out.println("</table>");
    }

    private void printHtmlPreface(HttpServletResponse resp) throws IOException {
        resp.setStatus(200);
        PrintWriter out = resp.getWriter();
        resp.setContentType("text/html");
        out.println("<html>");
        out.println("<head>");
        out.println("<style type=text/css>");
        out.println("p, table.popup-evals td { font-family: sans-serif; vertical-align: top }");
        out.println(".timestamp { font-size: x-small }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
    }

    @Override
    protected void handlePost(PersistenceManager pm, HttpServletRequest req, HttpServletResponse resp, String uri)
            throws IOException {
    }
}
