package edu.umd.cs.findbugs.flybush;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jdo.Query;

import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public class UsageDataConsolidator {
    private final PersistenceHelper persistenceHelper;
    public Multimap<String, Long> uuidsByPlugin = newHashSetMultiMap();
    public Multimap<String, Long> uuidsByAppName = newHashSetMultiMap();
    public Multimap<String, Long> uuidsByEntryPoint = newHashSetMultiMap();
    public Multimap<String, Long> uuidsByLanguage = newHashSetMultiMap();
    public Map<String, Multimap<String, Long>> uuidsByPluginVersion = Maps.newHashMap();
    public Multimap<String, Long> uuidsByVersion = newHashSetMultiMap();
    public Multimap<String, Long> uuidsByCountry = newHashSetMultiMap();
    public Multimap<String, Long> uuidsByOs = newHashSetMultiMap();
    public Multimap<String, Long> uuidsByJavaVersion = newHashSetMultiMap();
    public Set<Long> uuidsByDay = Sets.newHashSet();
    public Set<String> ipsByDay = Sets.newHashSet();

    UsageDataConsolidator(PersistenceHelper persistenceHelper) {
        this.persistenceHelper = persistenceHelper;
    }

    private static <K,V> SetMultimap<K, V> newHashSetMultiMap() {
        return Multimaps.newSetMultimap(Maps.<K, Collection<V>>newHashMap(),
                new Supplier<Set<V>>() {
                    public Set<V> get() {
                        return Sets.newHashSet();
                    }
                });
    }

    private DbUsageSummary createSummaryEntry(Date date, String category, String categoryKey, String categorySubkey, int value) {
        DbUsageSummary entry = persistenceHelper.createDbUsageSummary();
        entry.setDate(date);
        entry.setCategory(category);
        entry.setCategoryKey(categoryKey);
        entry.setCategorySubkey(categorySubkey);
        entry.setValue(value);
        entry.setLastUpdated(new Date());
        return entry;
    }

    public void process(Query query, List<DbUsageEntry> entries) {
//        int size = entries.size();
//        AbstractFlybushServlet.LOGGER.info("Total entries: " + size);
        int count = 0;
        for (DbUsageEntry entry : entries) {
            if (++count % 1000 == 0) {
                AbstractFlybushServlet.LOGGER.info("Processed " + count);
//                        + " of " + size + " - "
//                        + String.format("%.2f", count * 100.0 / size) + "%");
            }
            String uuidStr = entry.getUuid();
            Long uuid = Long.parseLong(uuidStr, 16);
            uuidsByDay.add(uuid);
            ipsByDay.add(entry.getIpAddress());
            String pluginFqn = entry.getPlugin();
            boolean corePlugin = "edu.umd.cs.findbugs.plugins.core".equals(pluginFqn);
            if (!corePlugin && pluginFqn != null && !pluginFqn.equals("")) {
                uuidsByPlugin.put(pluginFqn, uuid);
                String pluginVersion = entry.getPluginVersion();
                if (pluginVersion != null) {
                    Multimap<String, Long> byPlugin = uuidsByPluginVersion.get(pluginFqn);
                    if (byPlugin == null) {
                        byPlugin = newHashSetMultiMap();
                        uuidsByPluginVersion.put(pluginFqn, byPlugin);
                    }
                    byPlugin.put(pluginVersion, uuid);
                }
            }
            uuidsByVersion.put(entry.getVersion(), uuid);
            String country = entry.getCountry();
            if (country != null)
                uuidsByCountry.put(country.toLowerCase(), uuid);
            String language = entry.getLanguage();
            if (language != null)
                uuidsByLanguage.put(language.toLowerCase(), uuid);
            uuidsByOs.put(entry.getOs(), uuid);
            uuidsByJavaVersion.put(entry.getJavaVersion(), uuid);
            uuidsByAppName.put(entry.getAppName(), uuid);
            uuidsByEntryPoint.put(entry.getEntryPoint(), uuid);
        }


        query.closeAll();
    }

    public Set<DbUsageSummary> createSummaryEntries(Date date) {
        Set<DbUsageSummary> list = Sets.newHashSet();

        list.add(createSummaryEntry(date, "ips", null, null, ipsByDay.size()));

        list.add(createSummaryEntry(date, "users", null, null, uuidsByDay.size()));

        createSummaries(date, list, "version", uuidsByVersion);
        createSummaries(date, list, "plugin", uuidsByPlugin);
        createSummaries(date, list, "appName", uuidsByAppName);
        createSummaries(date, list, "entryPoint", uuidsByEntryPoint);

        createSummaries(date, list, "country", uuidsByCountry);
        createSummaries(date, list, "language", uuidsByLanguage);
        createSummaries(date, list, "os", uuidsByOs);
        createSummaries(date, list, "javaVersion", uuidsByJavaVersion);

        for (Map.Entry<String, Multimap<String, Long>> pentry : uuidsByPluginVersion.entrySet()) {
            for (Map.Entry<String, Collection<Long>> ventry : pentry.getValue().asMap().entrySet()) {
                list.add(createSummaryEntry(date, "pluginVersion", pentry.getKey(),
                        ventry.getKey(), ventry.getValue().size()));
            }
        }

        list.add(createSummaryEntry(date, "consolidation-data-version", null, null, UsageConsolidatorServlet.CONSOLIDATION_DATA_VERSION));
        return list;
    }

    private <E> void createSummaries(Date dateDate, Set<DbUsageSummary> list, String category, Multimap<String, E> map) {
        for (Map.Entry<String, Collection<E>> entry : map.asMap().entrySet()) {
            list.add(createSummaryEntry(dateDate, category, entry.getKey(), null, entry.getValue().size()));
        }
    }
}
