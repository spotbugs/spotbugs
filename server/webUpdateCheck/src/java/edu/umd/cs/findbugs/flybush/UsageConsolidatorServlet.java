package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Maps;

public class UsageConsolidatorServlet extends AbstractFlybushUpdateServlet {

    /**
     * update this when adding a new DbUsageSummary field, or when changing / fixing
     * bugs in the UsageDataConsolidator
     */
    public static final int CONSOLIDATION_DATA_VERSION = 13;

    public static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.ENGLISH);

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String uri = req.getRequestURI();
        PersistenceManager pm = getPersistenceManager();
        try {
            if (uri.equals("/consolidate-usage")) {
                if (true) {
                    LOGGER.severe("consolidate-usage request: " + uri);
                    show404(resp);
                    return;
                }
                
                String dateStr = req.getParameter("dateStart");
                String dateEndStr = req.getParameter("dateEnd");
                if (dateStr == null || dateStr.equals("")) {
                    startConsolidationTasks(resp);
                } else {
                    consolidateDate(resp, pm, dateStr, dateEndStr);
                }

            } else {
                LOGGER.warning("Unknown request: " + uri);
                show404(resp);
            }
        } finally {
            pm.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

    @Override
    protected void handlePost(PersistenceManager pm, HttpServletRequest req, HttpServletResponse resp, String uri)
            throws IOException {
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

    private void consolidateDate(HttpServletResponse resp, PersistenceManager pm, String dateStr, String dateEndStr)
            throws IOException {
        LOGGER.info("Consolidating " + dateStr + " - " + (dateEndStr == null ? "full day" : dateEndStr));
        Date startDate;
        try {
            startDate = DATE_FORMAT.parse(dateStr);
        } catch (ParseException e) {
            show404(resp);
            return;
        }
        Date endDate;
        try {
            endDate = dateEndStr == null ? null : DATE_FORMAT.parse(dateEndStr);
        } catch (ParseException e) {
            show404(resp);
            return;
        }

        if (endDate == null) {
            if (startDate.after(dayStart(new Date()))) {
                String msg = "SHOULD NOT BE CONSOLIDATING " + dateStr + " UNTIL END OF DAY! WTF??";
                LOGGER.warning(msg);
                setResponse(resp, 200, msg);
                return;
            }
        } else if (endDate.after(new Date())) {
            String msg = "SHOULD NOT BE CONSOLIDATING " + dateStr + " UNTIL " + dateEndStr + "! WTF??";
            LOGGER.warning(msg);
            setResponse(resp, 200, msg);
            return;
        }

        if (checkVersion(resp, pm, startDate, endDate))
            return;

        if (endDate != null) {
            LOGGER.info("Looking for usage data from " + dateStr + " to " + dateEndStr + " - initial query starting");
            Query query = pm.newQuery("select from " + persistenceHelper.getDbUsageEntryClassname()
                    + " where date >= :startDate && date < :endDate");
            query.addExtension("javax.persistence.query.chunkSize", 50);
            @SuppressWarnings("unchecked")
            List<DbUsageEntry> entries = (List<DbUsageEntry>) query.execute(startDate, endDate);
    
            UsageDataConsolidator data = new UsageDataConsolidator(persistenceHelper);
            data.process(query, entries);
            LOGGER.info("Storing consolidated data...");
    
            for (DbUsageSummary summary : data.createSummaryEntries(startDate, endDate)) {
                commit(pm, summary);
            }
    
            setResponse(resp, 200, "Done!");
            
        } else {
            Query vquery = pm.newQuery("select from " + persistenceHelper.getDbUsageSummaryClassname()
                    + " where date >= :date && date < :endDate");
            @SuppressWarnings("unchecked")
            List<DbUsageSummary> entries = (List<DbUsageSummary>) vquery.execute(startDate, nextDay(startDate));

            UsageDataConsolidator data = new UsageDataConsolidator(persistenceHelper);
            data.processSummaries(vquery, entries);
            
            for (DbUsageSummary summary : data.createSummaryEntries(startDate, null)) {
                commit(pm, summary);
            }

            setResponse(resp, 200, "Done!");
        }
    }

    private boolean checkVersion(HttpServletResponse resp, PersistenceManager pm, Date startDate, Date endDate) throws IOException {
        Query vquery = pm.newQuery("select from " + persistenceHelper.getDbUsageSummaryClassname()
                + " where date == :date && endDate == :endDate && category == 'consolidation-data-version'");
        @SuppressWarnings("unchecked")
        List<DbUsageSummary> ventries = (List<DbUsageSummary>) vquery.execute(startDate, endDate, CONSOLIDATION_DATA_VERSION);
        if (!ventries.isEmpty()) {
            for (DbUsageSummary ventry : ventries) {
                String lastUpdatedStr = DateFormat.getDateTimeInstance().format(ventry.getLastUpdated());
                if (ventry.getValue() == CONSOLIDATION_DATA_VERSION) {
                    int version = ventry.getValue();
                    String msg = "Skipping - already at version " + version + " - last updated " + lastUpdatedStr;
                    LOGGER.info(msg);
                    setResponse(resp, 200, msg);
                    return true;
                }
                LOGGER.warning("Found old data - version " + ventry.getValue() + " from " + lastUpdatedStr);
            }
            LOGGER.info("Deleting old data...");
            // the day summary should only delete old day summaries
            //TODO: wait is this ok?
            String endDateClause;
            if (endDate == null)
                endDateClause = "endDate == null";
            else
                endDateClause = "endDate != null";
            Query dquery = pm.newQuery("select from " + persistenceHelper.getDbUsageSummaryClassname()
                    + " where date >= :date && date < :date2");
            Date actualEndDate = endDate;
            if (endDate == null)
                actualEndDate = nextDay(startDate);
            long deleted = dquery.deletePersistentAll(startDate, actualEndDate);
            LOGGER.info("Deleted " + deleted + " entries");
            
        } else {
            LOGGER.info("No data found for " + startDate + " - " + endDate + ", proceeding to consolidate");
        }
        vquery.closeAll();
        return false;
    }

    private Date nextDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, 1);
        return cal.getTime();
    }

    private static void commit(PersistenceManager pm, DbUsageSummary entry) {
        pm.currentTransaction().begin();
        try {
            pm.makePersistent(entry);
            pm.currentTransaction().commit();
        } finally {
            if (pm.currentTransaction().isActive())
                pm.currentTransaction().rollback();
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

    private void startConsolidationTasks(HttpServletResponse resp) throws IOException {
        LOGGER.info("About to execute query");
        Calendar cal = yesterdayMidnight();
        //TODO: change back to six months!
        int SIX_MONTHS = 30;
        for (int i = 0; i < SIX_MONTHS; i++) {
            for (int j = 0; j < 12; j++) {
                Calendar hr = (Calendar) cal.clone();
                hr.add(Calendar.HOUR, 2*j);
                Calendar end = (Calendar) hr.clone();
                end.add(Calendar.HOUR, 2);
                enqueue(hr.getTime(), end.getTime());
            }

            // then enqueue the whole-day consolidation
            enqueue(cal.getTime(), null);
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }

        setResponse(resp, 200, "Done");
    }

    private void enqueue(Date startTime, Date endTime) {
        Map<String, String> parameters = Maps.newHashMap();
        parameters.put("dateStart", DATE_FORMAT.format(startTime));
        if (endTime != null)
            parameters.put("dateEnd", DATE_FORMAT.format(endTime));
        persistenceHelper.addToQueue("/consolidate-usage", parameters);
    }

    private Calendar yesterdayMidnight() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        return cal;
    }

}
