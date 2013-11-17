package edu.umd.cs.findbugs.flybush;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.Query;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public class UsageDataConsolidator {
    protected static final Logger LOGGER = Logger.getLogger(UsageDataConsolidator.class.getName());
    
    private final UsagePersistenceHelper persistenceHelper;
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

    public UsageDataConsolidator(UsagePersistenceHelper persistenceHelper) {
        this.persistenceHelper = persistenceHelper;
    }

    private static <K,V> SetMultimap<K, V> newHashSetMultiMap() {
        return Multimaps.newSetMultimap(Maps.<K, Collection<V>>newHashMap(),
                new Supplier<Set<V>>() {
                    @Override
                    public Set<V> get() {
                        return Sets.newHashSet();
                    }
                });
    }

    private DbUsageSummary createSummaryEntryWithValue(Date date, Date endDate, String category, String categoryKey, 
                                                       String categorySubkey, int value) {
        DbUsageSummary entry = persistenceHelper.createDbUsageSummary();
        entry.setDate(date);
        entry.setEndDate(endDate);
        entry.setCategory(category);
        entry.setCategoryKey(categoryKey);
        entry.setCategorySubkey(categorySubkey);
        entry.setValue(value);
        entry.setLastUpdated(new Date());
        return entry;
    }

    private DbUsageSummary createSummaryEntryWithIps(Date date, Date endDate, String category, String categoryKey, 
                                                     String categorySubkey, Collection<String> ipAddresses) {
        DbUsageSummary entry = createSummaryEntryWithValue(date, endDate, category, categoryKey, categorySubkey, ipAddresses.size());
        if (endDate != null)
            entry.setBlob(createIpBlob(ipAddresses));
        return entry;
    }

    private DbUsageSummary createSummaryEntryWithUuids(Date date, Date endDate, String category, String categoryKey, 
                                                       String categorySubkey, Collection<Long> uuids) {
        DbUsageSummary entry = createSummaryEntryWithValue(date,endDate,category,categoryKey,categorySubkey,uuids.size());
        if (endDate != null)
            entry.setBlob(createUuidBlob(uuids));
        return entry;
    }

    @SuppressWarnings("ConstantConditions")
    private byte[] createIpBlob(Collection<String> ips) {
        byte[] out = new byte[ips.size()*4];
        int count = 0;
        for (String ip : ips) {
            try {
                System.arraycopy(InetAddress.getByName(ip).getAddress(), 0, out, count, 4);
            } catch (Throwable e) {
                LOGGER.log(Level.WARNING, "Error parsing IP address " + ip, e);
            }
            count += 4;
        }
        return out;
    }

    private byte[] createUuidBlob(Collection<Long> uuids) {
        long[] data = new long[uuids.size()];
        int count = 0;
        for (Long uuid : uuids) {
            data[count++] = uuid;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) (data.length * 1.5));
        try {
            ObjectOutputStream stream = new ObjectOutputStream(out);
            stream.writeObject(data);
            stream.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while writing " + uuids.size() +" UUID's", e);
            return null;
        }
        return out.toByteArray();
    }

    public void process(Query query, List<DbUsageEntry> entries) {
        int count = 0;
        for (DbUsageEntry entry : entries) {
            if (++count % 1000 == 0) {
                AbstractFlybushUpdateServlet.LOGGER.info("Processed " + count);
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

    public void processSummaries(Query vquery, List<DbUsageSummary> entries) {
        for (DbUsageSummary summary : entries) {
            if (summary.getEndDate() == null)
                continue;
            String categoryKey = summary.getCategoryKey();
            if (summary.getCategory().equals("users"))
                uuidsByDay.addAll(decodeUuidBlob(summary.getBlob()));

            if (summary.getCategory().equals("ips"))
                ipsByDay.addAll(decodeIpBlob(summary.getBlob()));

            if (summary.getCategory().equals("version"))
                uuidsByVersion.putAll(categoryKey, decodeUuidBlob(summary.getBlob()));
            if (summary.getCategory().equals("plugin"))
                uuidsByPlugin.putAll(categoryKey, decodeUuidBlob(summary.getBlob()));
            if (summary.getCategory().equals("country"))
                uuidsByCountry.putAll(categoryKey, decodeUuidBlob(summary.getBlob()));
            if (summary.getCategory().equals("language"))
                uuidsByLanguage.putAll(categoryKey, decodeUuidBlob(summary.getBlob()));
            if (summary.getCategory().equals("javaVersion"))
                uuidsByJavaVersion.putAll(categoryKey, decodeUuidBlob(summary.getBlob()));
            if (summary.getCategory().equals("os"))
                uuidsByOs.putAll(categoryKey, decodeUuidBlob(summary.getBlob()));
            if (summary.getCategory().equals("appName"))
                uuidsByAppName.putAll(categoryKey, decodeUuidBlob(summary.getBlob()));
            if (summary.getCategory().equals("entryPoint"))
                uuidsByEntryPoint.putAll(categoryKey, decodeUuidBlob(summary.getBlob()));

        }
        vquery.closeAll();
    }

    @SuppressWarnings("ConstantConditions")
    private Collection<String> decodeIpBlob(byte[] blob) {
        Collection<String> list = Lists.newArrayList();
        for (int i = 0; i < blob.length; i += 4) {
            byte[] addr = new byte[4];
            System.arraycopy(blob, i, addr, 0, 4);
            try {
                String hostAddress = InetAddress.getByAddress(addr).getHostAddress();
                list.add(hostAddress);
            } catch (Exception e) {
                // skip
                LOGGER.log(Level.WARNING, "Error decoding IP address", e);
            }
        }
        return list;
    }

    private List<Long> decodeUuidBlob(byte[] blob) {
        List<Long> list = Lists.newArrayList();
        ObjectInputStream stream;
        try {
            stream = new ObjectInputStream(new ByteArrayInputStream(blob));
            long[] longs = (long[]) stream.readObject();
            for (long l : longs) {
                list.add(l);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error decoding blob", e);
        }
        return list;
    }

    public Set<DbUsageSummary> createSummaryEntries(Date date, Date endDate) {
        Set<DbUsageSummary> list = Sets.newHashSet();

        list.add(createSummaryEntryWithIps(date, endDate, "ips", null, null, ipsByDay));

        list.add(createSummaryEntryWithUuids(date, endDate, "users", null, null, uuidsByDay));

        createSummaries(date, endDate, list, "version", uuidsByVersion);
        createSummaries(date, endDate, list, "plugin", uuidsByPlugin);
        createSummaries(date, endDate, list, "appName", uuidsByAppName);
        createSummaries(date, endDate, list, "entryPoint", uuidsByEntryPoint);

        createSummaries(date, endDate, list, "country", uuidsByCountry);
        createSummaries(date, endDate, list, "language", uuidsByLanguage);
        createSummaries(date, endDate, list, "os", uuidsByOs);
        createSummaries(date, endDate, list, "javaVersion", uuidsByJavaVersion);

        for (Map.Entry<String, Multimap<String, Long>> pentry : uuidsByPluginVersion.entrySet()) {
            for (Map.Entry<String, Collection<Long>> ventry : pentry.getValue().asMap().entrySet()) {
                list.add(createSummaryEntryWithUuids(date, endDate, "pluginVersion", pentry.getKey(),
                        ventry.getKey(), ventry.getValue()));
            }
        }

        list.add(createSummaryEntryWithValue(date, endDate, "consolidation-data-version", null, null,
                UsageConsolidatorServlet.CONSOLIDATION_DATA_VERSION));
        return list;
    }

    private void createSummaries(Date date, Date endDate, Set<DbUsageSummary> list, String category,
                                     Multimap<String, Long> map) {
        for (Map.Entry<String, Collection<Long>> entry : map.asMap().entrySet()) {
            list.add(createSummaryEntryWithUuids(date, endDate, category, entry.getKey(), null, entry.getValue()));
        }
    }
}
