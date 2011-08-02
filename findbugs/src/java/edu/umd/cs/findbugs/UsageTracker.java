package edu.umd.cs.findbugs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.WillClose;

import edu.umd.cs.findbugs.util.MultiMap;
import edu.umd.cs.findbugs.util.Util;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class UsageTracker {
    private static final Logger LOGGER = Logger.getLogger(UsageTracker.class.getName());
    private static final String KEY_DISABLE_ALL_USAGE_TRACKING = "disableAllUsageTracking";
    private static final String KEY_REDIRECT_ALL_USAGE_TRACKING = "redirectAllUsageTracking";
    
    private final UsageTrackerCallback dfc;
    private List<PluginUpdate> pluginUpdates = new ArrayList<PluginUpdate>();

    public UsageTracker(UsageTrackerCallback dfc) {
        this.dfc = dfc;
    }

    void trackUsage(Collection<Plugin> plugins) {
        if (trackingIsGloballyDisabled())
            return;

        String redirect = dfc.getGlobalOption(KEY_REDIRECT_ALL_USAGE_TRACKING);
        Plugin setter = dfc.getGlobalOptionSetter(KEY_REDIRECT_ALL_USAGE_TRACKING);
        URI redirectUri = null;
        String pluginName = setter == null ? "<unknown plugin>" : setter.getShortDescription();
        if (redirect != null && !redirect.trim().equals("")) {
            try {
                redirectUri = new URI(redirect);
            } catch (URISyntaxException e) {
                String error = "Invalid usage tracking redirect URI in " + pluginName + ": " + redirect;
                logError(Level.SEVERE, error);
                throw new IllegalStateException(error);
            }
        }

        final CountDownLatch latch;
        if (redirectUri != null) {
            latch = new CountDownLatch(1);
            logError(Level.INFO, "Redirecting all plugin usage tracking to " + redirectUri + " (" + pluginName + ")");
            startUsageReportThread(redirectUri, plugins, latch);
        } else {
            MultiMap<URI,Plugin> pluginsByTracker = new MultiMap<URI, Plugin>(HashSet.class);
            for (Plugin plugin : plugins) {
                URI uri = plugin.getUsageTracker();
                if (uri == null) {
                    logError(Level.FINE, "Not logging usage for " + plugin.getShortDescription()
                            + " - no usageTracker attribute in plugin XML file");
                    continue;
                }
                pluginsByTracker.add(uri, plugin);
            }
            latch = new CountDownLatch(pluginsByTracker.keySet().size());
            for (URI uri : pluginsByTracker.keySet()) {
                startUsageReportThread(uri, pluginsByTracker.get(uri), latch);
            }
        }
        new Thread(new Runnable() {
            public void run() {
                try {
                    latch.await();
                    dfc.pluginUpdateCheckComplete(pluginUpdates);
                } catch (Exception e) {
                }

            }
        }, "Usage tracker").start();
    }

    private boolean trackingIsGloballyDisabled() {
        String disable = dfc.getGlobalOption(KEY_DISABLE_ALL_USAGE_TRACKING);
        Plugin setter = dfc.getGlobalOptionSetter(KEY_DISABLE_ALL_USAGE_TRACKING);
        String pluginName = setter == null ? "<unknown plugin>" : setter.getShortDescription();
        if ("true".equalsIgnoreCase(disable)) {
            logError(Level.INFO, "Skipping usage tracking due to disableAllUsageTracking=true set by "
                    + pluginName);
            return true;
        }
        if (disable != null && !"false".equalsIgnoreCase(disable)) {
            String error = "Unknown value '" + disable + "' for disableAllUsageTracking in " + pluginName;
            logError(Level.SEVERE, error);
            throw new IllegalStateException(error);
        }
        return false;
    }

    private void startUsageReportThread(final URI trackerUrl, final Collection<Plugin> plugins, final CountDownLatch latch) {
        if (trackerUrl == null) {
            logError(Level.INFO, "Not submitting usage tracking for plugins with blank URL: " + getPluginNames(plugins));
            return;
        }
        final String entryPoint = getEntryPoint();
        if ((entryPoint.contains("edu.umd.cs.findbugs.FindBugsTestCase") 
                || entryPoint.contains("edu.umd.cs.findbugs.cloud.appEngine.AbstractWebCloudTest"))
                && (trackerUrl.getScheme().equals("http") || trackerUrl.getScheme().equals("https"))) {
            LOGGER.fine("Skipping usage tracking because we're running in FindBugsTestCase and using "
                    + trackerUrl.getScheme());
            return;
        }
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    actuallyTrackUsage(trackerUrl, plugins, entryPoint);
                    latch.countDown();
                } catch (Exception e) {
                    logError(e, "Error submitting usage tracking data to " + trackerUrl);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void actuallyTrackUsage(URI trackerUrl, Collection<Plugin> plugins, String entryPoint) throws IOException {
        LOGGER.fine("Submitting anonymous usage tracking info to " + trackerUrl
                + " for " + getPluginNames(plugins));
        HttpURLConnection conn = (HttpURLConnection) trackerUrl.toURL().openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.connect();
        OutputStream out = conn.getOutputStream();
        OutputStreamXMLOutput xmlOutput = new OutputStreamXMLOutput(out);
        xmlOutput.beginDocument();
        xmlOutput.startTag("findbugs-invocation");
        xmlOutput.addAttribute("version", Version.RELEASE);
        String applicationName = Version.getApplicationName();
        if (applicationName == null || applicationName.equals("")) {
            int lastDot = entryPoint.lastIndexOf('.');
            if (lastDot == -1)
                applicationName = entryPoint;
            else
                applicationName = entryPoint.substring(lastDot + 1);
        }
        xmlOutput.addAttribute("app-name", applicationName);
        String applicationVersion = Version.getApplicationVersion();
        if (applicationVersion == null)
            applicationVersion = "";
        xmlOutput.addAttribute("app-version", applicationVersion);
        xmlOutput.addAttribute("entry-point", entryPoint);
        xmlOutput.addAttribute("os", SystemProperties.getProperty("os.name", ""));
        xmlOutput.addAttribute("java-version", getMajorJavaVersion());
        Locale locale = Locale.getDefault();
        xmlOutput.addAttribute("language", locale.getLanguage());
        xmlOutput.addAttribute("country", locale.getCountry());
        xmlOutput.addAttribute("uuid", getUuid());
        xmlOutput.stopTag(false);
        for (Plugin plugin : plugins) {
            xmlOutput.startTag("plugin");
            xmlOutput.addAttribute("id", plugin.getPluginId());
            xmlOutput.addAttribute("name", plugin.getShortDescription());
            xmlOutput.addAttribute("version", plugin.getVersion());
            xmlOutput.stopTag(true);
        }

        xmlOutput.closeTag("findbugs-invocation");
        xmlOutput.flush();
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            logError(SystemProperties.ASSERTIONS_ENABLED ? Level.WARNING : Level.FINE,
                    "Error submitting anonymous usage data to " + trackerUrl + ": "
                    + responseCode + " - " + conn.getResponseMessage());
        }
        parseUpdateXml(trackerUrl, plugins, conn.getInputStream());
        xmlOutput.finish();
        conn.disconnect();
        
    }

    // package-private for testing
    @SuppressWarnings({"unchecked"})
    void parseUpdateXml(URI trackerUrl, Collection<Plugin> plugins, @WillClose InputStream inputStream) {
        try {
            Document doc = new SAXReader().read(inputStream);
            List<Element> pluginEls = (List<Element>) doc.selectNodes("fb-plugin-updates/plugin");
            for (Element pluginEl : pluginEls) {
                String id = pluginEl.attributeValue("id");
                for (Plugin plugin : plugins) {
                    if (plugin.getPluginId().equals(id)) {
                        checkPlugin(pluginEl, plugin);
                    }
                }
            }
        } catch (Exception e) {
            logError(e, "Could not parse plugin version update for " + trackerUrl);
        } finally {
            Util.closeSilently(inputStream);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void checkPlugin(Element pluginEl, Plugin plugin) throws ParseException {
        Date max = null;
        Element maxEl = null;
        for (Element release : (List<Element>) pluginEl.elements("release")) {
            Date date = parseReleaseDate(release);
            if (max == null || date.after(max)) {
                max = date;
                maxEl = release;
            }
        }
        if (maxEl != null) {
            printPluginUpdateMsg(plugin, maxEl);
        }
    }

    private void printPluginUpdateMsg(Plugin plugin, Element maxEl) throws ParseException {
        String version = maxEl.attributeValue("version");
        if (version.equals(plugin.getVersion()))
            return;

        String url = maxEl.attributeValue("url");

        String message = maxEl.element("message").getTextTrim();
        Date date = parseReleaseDate(maxEl);
        Date releaseDate = plugin.getReleaseDate();
        if (releaseDate == null || date.after(releaseDate)) {
            pluginUpdates.add(new PluginUpdate(plugin, version, url, message));
        }
    }

    // protected for testing
    protected void logError(Level level, String msg) {
        LOGGER.log(level, msg);
    }

    // protected for testing
    protected void logError(Exception e, String msg) {
        LOGGER.log(Level.INFO, msg, e);
    }

    private Date parseReleaseDate(Element releaseEl) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm aa z");
        String dateStr = releaseEl.attributeValue("date");
        try {
            return format.parse(dateStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing " + dateStr, e);
        }
    }

    private String getPluginNames(Collection<Plugin> plugins) {
        String text = "";
        boolean first = true;
        for (Plugin plugin : plugins) {
            text = (first ? "" : ", ") + plugin.getShortDescription();
            first = false;
        }
        return text;
    }

    private String getEntryPoint() {
        String lastFbClass = "<UNKNOWN>";
        for (StackTraceElement s : Thread.currentThread().getStackTrace()) {
            String cls = s.getClassName();
            if (cls.startsWith("edu.umd.cs.findbugs.")) {
                lastFbClass = cls;
            }
        }
        return lastFbClass;
    }

    private static synchronized String getUuid() {
        Preferences prefs = Preferences.userNodeForPackage(UsageTracker.class);
        long uuid = prefs.getLong("uuid", 0);
        if (uuid == 0) {
            uuid = new Random().nextLong();
            prefs.putLong("uuid", uuid);
        }
        return Long.toString(uuid, 16);
    }

    private String getMajorJavaVersion() {
        String ver = SystemProperties.getProperty("java.version", "");
        Matcher m = Pattern.compile("^\\d+\\.\\d+").matcher(ver);
        if (m.find()) {
           return m.group();
        }
        return "";
    }

    public static class PluginUpdate {
        private final Plugin plugin;
        private final String version;
        private final String url;
        private final String message;

        private PluginUpdate(Plugin plugin, String version, String url, String message) {
            this.plugin = plugin;
            this.version = version;
            this.url = url;
            this.message = message;
        }

        public Plugin getPlugin() {
            return plugin;
        }

        public String getVersion() {
            return version;
        }

        public String getUrl() {
            return url;
        }

        public String getMessage() {
            return message;
        }
    }
}
