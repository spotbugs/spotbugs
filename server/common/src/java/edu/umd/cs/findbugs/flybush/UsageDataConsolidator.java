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
    public Multimap<String, String> uuidsByPlugin;
    public Map<String, Multimap<String, String>> uuidsByPluginVersion;
    public Multimap<String, String> uuidsByVersion;
    public Multimap<String, String> uuidsByCountry;
    public Multimap<String, String> uuidsByOs;
    public Multimap<String, String> uuidsByJavaVersion;
    public Set<String> uuidsByDay;
    public Set<String> ipsByDay;

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
        Multimap<String, DbUsageEntry> entriesByUuid = newHashSetMultiMap();
        Multimap<String, String> pluginsByUuid = newHashSetMultiMap();
        uuidsByPlugin = newHashSetMultiMap();
        uuidsByPluginVersion = Maps.newHashMap();
        uuidsByVersion = newHashSetMultiMap();
        uuidsByCountry = newHashSetMultiMap();
        Multimap<String, String> uuidsByLanguage = newHashSetMultiMap();
        uuidsByOs = newHashSetMultiMap();
        uuidsByJavaVersion = newHashSetMultiMap();
        uuidsByDay = Sets.newHashSet();
        ipsByDay = Sets.newHashSet();
        int size = entries.size();
        AbstractFlybushServlet.LOGGER.info("Total entries: " + size);
        int count = 0;
        for (DbUsageEntry entry : entries) {
            if (++count % 1000 == 0) {
                AbstractFlybushServlet.LOGGER.info("Processed " + count + " of " + size + " - " + String.format("%.2f", count * 100.0 / size) + "%");
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
    }

    public Set<DbUsageSummary> createSummaryEntries(Date dateDate) {
        Set<DbUsageSummary> list = Sets.newHashSet();

        list.add(createSummaryEntry(dateDate, "ips", null, null, ipsByDay.size()));

        list.add(createSummaryEntry(dateDate, "users", null, null, uuidsByDay.size()));

        for (Map.Entry<String, Collection<String>> entry : uuidsByVersion.asMap().entrySet()) {
            list.add(createSummaryEntry(dateDate, "version", entry.getKey(), null, entry.getValue().size()));
        }

        for (Map.Entry<String, Collection<String>> entry : uuidsByPlugin.asMap().entrySet()) {
            list.add(createSummaryEntry(dateDate, "plugin", entry.getKey(), null, entry.getValue().size()));
        }
        for (Map.Entry<String, Collection<String>> entry : uuidsByCountry.asMap().entrySet()) {
            list.add(createSummaryEntry(dateDate, "country", entry.getKey(), null, entry.getValue().size()));
        }

        for (Map.Entry<String, Collection<String>> entry : uuidsByOs.asMap().entrySet()) {
            list.add(createSummaryEntry(dateDate, "os", entry.getKey(), null, entry.getValue().size()));
        }

        for (Map.Entry<String, Collection<String>> entry : uuidsByJavaVersion.asMap().entrySet()) {
            list.add(createSummaryEntry(dateDate, "javaVersion", entry.getKey(), null, entry.getValue().size()));
        }

        for (Map.Entry<String, Multimap<String, String>> pentry : uuidsByPluginVersion.entrySet()) {
            for (Map.Entry<String, Collection<String>> ventry : pentry.getValue().asMap().entrySet()) {
                list.add(createSummaryEntry(dateDate, "pluginVersion", pentry.getKey(),
                        ventry.getKey(), ventry.getValue().size()));
            }
        }

        list.add(createSummaryEntry(dateDate, "consolidation-data-version", null, null, UsageConsolidatorServlet.CONSOLIDATION_DATA_VERSION));
        return list;
    }
}
