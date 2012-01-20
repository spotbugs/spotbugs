package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public class UsageConsolidatorServlet extends AbstractFlybushServlet {


    public static final int CONSOLIDATION_DATA_VERSION = 1;
    public static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT, Locale.ENGLISH);

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String uri = req.getRequestURI();
        PersistenceManager pm = getPersistenceManager();
        try {
            if (uri.equals("/consolidate-usage")) {
                String dateStr = req.getParameter("date");
                if (dateStr == null || dateStr.equals("")) {
                    startConsolidating(resp);
                } else {
                    consolidateDate(req, resp, pm, dateStr);
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

    private static <K,V> SetMultimap<K, V> newHashSetMultiMap() {
        return Multimaps.newSetMultimap(Maps.<K, Collection<V>>newHashMap(),
                new Supplier<Set<V>>() {
                    public Set<V> get() {
                        return Sets.newHashSet();
                    }
                });
    }

    private void consolidateDate(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm, String dateStr) 
            throws IOException {
        LOGGER.warning("Consolidating " + dateStr);
        Date dateDate;
        try {
            dateDate = dayStart(DATE_FORMAT.parse(dateStr));
        } catch (ParseException e) {
            show404(resp);
            return;
        }

        Query vquery = pm.newQuery("select from " + persistenceHelper.getDbUsageSummaryClassname() 
                + " where date == :date && category == 'consolidation-data-version' && value == :ver");
        @SuppressWarnings("unchecked") 
        List<DbUsageSummary> ventries = (List<DbUsageSummary>) vquery.execute(dateDate, CONSOLIDATION_DATA_VERSION);
        if (!ventries.isEmpty()) {
            DbUsageSummary summary = ventries.iterator().next();
            String lastUpdatedStr = DateFormat.getDateTimeInstance().format(summary.getLastUpdated());
            int version = summary.getValue();
            String msg = "Skipping - already at version " + version + " - last updated " + lastUpdatedStr;
            LOGGER.warning(msg);
            setResponse(resp, 200, msg);
            return;
        }

        Query query = pm.newQuery("select from " + persistenceHelper.getDbUsageEntryClassname()
                + " where date >= :startDate && date < :endDate");
        @SuppressWarnings("unchecked")
        List<DbUsageEntry> entries = (List<DbUsageEntry>) query.execute(dateDate, nextDay(dateDate));

        Multimap<String, DbUsageEntry> entriesByUuid = newHashSetMultiMap();
        Multimap<String, String> pluginsByUuid = newHashSetMultiMap();
        Multimap<String, String> uuidsByPlugin = newHashSetMultiMap();
        Map<String, Multimap<String,String>> uuidsByPluginVersion = Maps.newHashMap();
        Multimap<String, String> uuidsByVersion = newHashSetMultiMap();
        Multimap<String, String> uuidsByCountry = newHashSetMultiMap();
        Multimap<String, String> uuidsByLanguage = newHashSetMultiMap();
        Multimap<String, String> uuidsByOs = newHashSetMultiMap();
        Multimap<String, String> uuidsByJavaVersion = newHashSetMultiMap();
        Set<String> uuidsByDay = Sets.newHashSet();
        Set<String> ipsByDay = Sets.newHashSet();
        int size = entries.size();
        LOGGER.warning("Total entries: " + size);
        int count = 0;
        for (DbUsageEntry entry : entries) {
            if (++count % 1000 == 0) {
                LOGGER.warning("Processed " + count + " of " + size + " - " + String.format("%.2f", count * 100.0 / size) + "%");
            }
            String uuid = entry.getUuid();
            uuidsByDay.add(uuid);
            ipsByDay.add(entry.getIpAddress());
            entriesByUuid.put(uuid, entry);
            String pluginFqn = entry.getPlugin();
            boolean corePlugin = "edu.umd.cs.findbugs.plugins.core".equals(pluginFqn);
            if (!corePlugin && pluginFqn != null && !pluginFqn.equals("")) {
                pluginsByUuid.put(uuid, pluginFqn);
                uuidsByPlugin.put(pluginFqn, uuid);
                String pluginVersion = entry.getPluginVersion();
                if (pluginVersion != null) {
                    Multimap<String, String> byPlugin = uuidsByPluginVersion.get(pluginFqn);
                    if (byPlugin == null) {
                        byPlugin = newHashSetMultiMap();
                        uuidsByPluginVersion.put(pluginFqn, byPlugin);
                    }
                    byPlugin.put(pluginVersion, uuid);
                }
            }
            uuidsByVersion.put(entry.getVersion(), uuid);
            uuidsByCountry.put(entry.getCountry().toLowerCase(), uuid);
            uuidsByLanguage.put(entry.getCountry().toLowerCase(), uuid);
            uuidsByOs.put(entry.getOs(), uuid);
            uuidsByJavaVersion.put(entry.getJavaVersion(), uuid);
        }


        query.closeAll();
        LOGGER.warning("Storing consolidated data...");

        storeUsageSummary(pm, dateDate, "ips", null, null, ipsByDay.size());

        storeUsageSummary(pm, dateDate, "users", null, null, uuidsByDay.size());

        for (Entry<String, Collection<String>> entry : uuidsByVersion.asMap().entrySet()) {
            storeUsageSummary(pm, dateDate, "version", entry.getKey(), null, entry.getValue().size());
        }

        for (Entry<String, Collection<String>> entry : uuidsByPlugin.asMap().entrySet()) {
            storeUsageSummary(pm, dateDate, "plugin", entry.getKey(), null, entry.getValue().size());
        }
        for (Entry<String, Collection<String>> entry : uuidsByCountry.asMap().entrySet()) {
            storeUsageSummary(pm, dateDate, "country", entry.getKey(), null, entry.getValue().size());
        }

        for (Entry<String, Collection<String>> entry : uuidsByOs.asMap().entrySet()) {
            storeUsageSummary(pm, dateDate, "os", entry.getKey(), null, entry.getValue().size());
        }

        for (Entry<String, Collection<String>> entry : uuidsByJavaVersion.asMap().entrySet()) {
            storeUsageSummary(pm, dateDate, "javaVersion", entry.getKey(), null, entry.getValue().size());
        }
        
        for (Entry<String, Multimap<String, String>> pentry : uuidsByPluginVersion.entrySet()) {
            for (Entry<String, Collection<String>> ventry : pentry.getValue().asMap().entrySet()) {
                storeUsageSummary(pm, dateDate, "pluginVersion", pentry.getKey(), ventry.getKey(), ventry.getValue().size());
            }
        }
        storeUsageSummary(pm, dateDate, "consolidation-data-version", null, null, CONSOLIDATION_DATA_VERSION);
        
        setResponse(resp, 200, "Done!");
    }

    private Date nextDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, 1);
        return cal.getTime();
    }

    private void storeUsageSummary(PersistenceManager pm, Date date, String category, String categoryKey,
                                   String categorySubkey, int value) {
        DbUsageSummary entry = persistenceHelper.createDbUsageSummary();
        entry.setDate(date);
        entry.setCategory(category);
        entry.setCategoryKey(categoryKey);
        entry.setCategorySubkey(categorySubkey);
        entry.setValue(value);
        entry.setLastUpdated(new Date());
        
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

    private void startConsolidating(HttpServletResponse resp) throws IOException {
        LOGGER.warning("About to execute query");
        Calendar cal = yesterdayMidnight();
        //TODO: change back to six months!
        int SIX_MONTHS = 30;
        for (int i = 0; i < SIX_MONTHS; i++) {
            Map<String, String> parameters = Maps.newHashMap();
            parameters.put("date", DATE_FORMAT.format(cal.getTime()));
            persistenceHelper.addToQueue("/consolidate-usage", parameters);
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        setResponse(resp, 200, "Done");
    }

    private Calendar yesterdayMidnight() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return cal;
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
}
