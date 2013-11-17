package edu.umd.cs.findbugs.flybush.appengine;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import edu.umd.cs.findbugs.flybush.DbPluginUpdateXml;
import edu.umd.cs.findbugs.flybush.DbUsageEntry;
import edu.umd.cs.findbugs.flybush.DbUsageSummary;
import edu.umd.cs.findbugs.flybush.UsagePersistenceHelper;

public class AppEnginePersistenceHelper extends UsagePersistenceHelper {
    private static final Logger LOGGER = Logger.getLogger(AppEnginePersistenceHelper.class.getName());
    private Random random = new Random();

    @Override
    public PersistenceManagerFactory getPersistenceManagerFactory() {
        return PMF.get();
    }

    @Override
    public PersistenceManager getPersistenceManager() {
        return getPersistenceManagerFactory().getPersistenceManager();
    }

    @Override
    public DbUsageEntry createDbUsageEntry() {
        return new AppEngineDbUsageEntry();
    }


    @Override
    public DbPluginUpdateXml createPluginUpdateXml() {
        return new AppEngineDbPluginUpdateXml();
    }

    @Override
    public DbUsageSummary createDbUsageSummary() {
        return new AppEngineDbUsageSummary();
    }

    @Override
    public int clearAllData() {
        int deleted = 0;
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pquery = ds.prepare(new Query().setKeysOnly());
        for (Entity entity : pquery.asIterable()) {
            ds.delete(entity.getKey());
            deleted++;
        }
        return deleted;
    }


    @Override
    public Class<? extends DbUsageEntry> getDbUsageEntryClass() {
        return AppEngineDbUsageEntry.class;
    }

    @Override
    public Class<? extends DbPluginUpdateXml> getDbPluginUpdateXmlClass() {
        return AppEngineDbPluginUpdateXml.class;
    }

    @Override
    public Class<? extends DbUsageSummary> getDbUsageSummaryClass() {
        return AppEngineDbUsageSummary.class;
    }

    @Override
    public <E> E getObjectById(PersistenceManager pm, Class<? extends E> cls, Object key) {
        return pm.getObjectById(cls, key);
    }


    /**
     * Uses App Engine's memcache implementation to determine whether a
     * given user's client version data has been logged today.
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public boolean shouldRecordClientStats(String ip, String appName, String appVer, long midnightToday) {
        Cache cache;

        try {
            cache = CacheManager.getInstance().getCacheFactory().createCache(Collections.emptyMap());
        } catch (CacheException e) {
            LOGGER.log(Level.WARNING, "memcache could not be initialized", e);
            return true; // to be safe
        }

        String id = "already-recorded-" + ip + "-" + appName + "-" + appVer + "-" + midnightToday;
        Boolean val = (Boolean) cache.get(id);
        if (val != null && val.equals(Boolean.TRUE)) // has already been recorded today
            return false;
        cache.put(id, Boolean.TRUE);
        return true;
    }

    @Override
    public void addToQueue(String url, Map<String, String> params) {
        Queue queue = QueueFactory.getDefaultQueue();
        TaskOptions taskOptions = withUrl(url);
        //[a-zA-Z\d_-]
        taskOptions.taskName((url + "--" + new TreeMap<String, String>(params).toString()).replaceAll("[^a-zA-Z\\d_-]", "_") + "__" + random.nextInt());
        for (Map.Entry<String, String> entry : params.entrySet()) {
            taskOptions.param(entry.getKey(), entry.getValue());
        }
        queue.add(taskOptions);
    }

    @Override
    public String getEmailOfCurrentAppengineUser() {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        if (user == null)
            return null;
        return user.getEmail();
    }

}
