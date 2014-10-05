package edu.umd.cs.findbugs.updates;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.WillClose;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.Version;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.util.MultiMap;
import edu.umd.cs.findbugs.util.Util;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLUtil;

public class UpdateChecker {

    public static final String PLUGIN_RELEASE_DATE_FMT = "MM/dd/yyyy hh:mm aa z";
    private static final Logger LOGGER = Logger.getLogger(UpdateChecker.class.getName());
    private static final String KEY_DISABLE_ALL_UPDATE_CHECKS = "noUpdateChecks";
    private static final String KEY_REDIRECT_ALL_UPDATE_CHECKS = "redirectUpdateChecks";
    private static final boolean ENV_FB_NO_UPDATE_CHECKS = System.getenv("FB_NO_UPDATE_CHECKS") != null;

    private final UpdateCheckCallback dfc;
    private final List<PluginUpdate> pluginUpdates = new CopyOnWriteArrayList<PluginUpdate>();

    public UpdateChecker(UpdateCheckCallback dfc) {
        this.dfc = dfc;
    }

    public void checkForUpdates(Collection<Plugin> plugins, final boolean force) {
        if (updateChecksGloballyDisabled()) {
            dfc.pluginUpdateCheckComplete(pluginUpdates, force);
            return;
        }

        URI redirectUri = getRedirectURL(force);

        final CountDownLatch latch;
        if (redirectUri != null) {
            latch = new CountDownLatch(1);
            startUpdateCheckThread(redirectUri, plugins, latch);
        } else {
            MultiMap<URI,Plugin> pluginsByUrl = new MultiMap<URI, Plugin>(HashSet.class);
            for (Plugin plugin : plugins) {
                URI uri = plugin.getUpdateUrl();
                if (uri == null) {
                    logError(Level.FINE, "Not checking for updates for " + plugin.getShortDescription()
                            + " - no update-url attribute in plugin XML file");
                    continue;
                }
                pluginsByUrl.add(uri, plugin);
            }
            latch = new CountDownLatch(pluginsByUrl.keySet().size());
            for (URI uri : pluginsByUrl.keySet()) {
                startUpdateCheckThread(uri, pluginsByUrl.get(uri), latch);
            }
        }

        waitForCompletion(latch, force);
    }

    public @CheckForNull URI getRedirectURL(final boolean force) {
        String redirect = dfc.getGlobalOption(KEY_REDIRECT_ALL_UPDATE_CHECKS);
        String sysprop = System.getProperty("findbugs.redirectUpdateChecks");
        if (sysprop != null) {
            redirect = sysprop;
        }
        Plugin setter = dfc.getGlobalOptionSetter(KEY_REDIRECT_ALL_UPDATE_CHECKS);
        URI redirectUri = null;
        String pluginName = setter == null ? "<unknown plugin>" : setter.getShortDescription();
        if (redirect != null && !"".equals(redirect.trim())) {
            try {
                redirectUri = new URI(redirect);
                logError(Level.INFO, "Redirecting all plugin update checks to " + redirectUri + " (" + pluginName + ")");

            } catch (URISyntaxException e) {
                String error = "Invalid update check redirect URI in " + pluginName + ": " + redirect;
                logError(Level.SEVERE, error);
                dfc.pluginUpdateCheckComplete(pluginUpdates, force);
                throw new IllegalStateException(error);
            }
        }
        return redirectUri;
    }

    private long dontWarnAgainUntil() {
        Preferences prefs = Preferences.userNodeForPackage(UpdateChecker.class);

        String oldSeen = prefs.get("last-plugin-update-seen", "");
        if (oldSeen == null || "".equals(oldSeen)) {
            return 0;
        }
        try {
            return Long.parseLong(oldSeen) + DONT_REMIND_WINDOW;
        } catch (Exception e) {
            return 0;
        }
    }
    static final long DONT_REMIND_WINDOW = 3L*24*60*60*1000;
    public boolean updatesHaveBeenSeenBefore(Collection<UpdateChecker.PluginUpdate> updates) {
        long now = System.currentTimeMillis();
        Preferences prefs = Preferences.userNodeForPackage(UpdateChecker.class);
        String oldHash = prefs.get("last-plugin-update-hash", "");

        String newHash = Integer.toString(buildPluginUpdateHash(updates));
        if (oldHash.equals(newHash) && dontWarnAgainUntil() > now) {
            LOGGER.fine("Skipping update dialog because these updates have been seen before");
            return true;
        }
        prefs.put("last-plugin-update-hash", newHash);
        prefs.put("last-plugin-update-seen", Long.toString(now));
        return false;
    }

    private int buildPluginUpdateHash(Collection<UpdateChecker.PluginUpdate> updates) {
        HashSet<String> builder = new HashSet<String>();
        for (UpdateChecker.PluginUpdate update : updates) {
            builder.add( update.getPlugin().getPluginId() + update.getVersion());
        }
        return builder.hashCode();
    }

    private void waitForCompletion(final CountDownLatch latch, final boolean force) {
        Util.runInDameonThread(new Runnable() {
            @Override
            public void run() {
                if (DEBUG) {
                    System.out.println("Checking for version updates");
                }
                try {
                    if (! latch.await(15, TimeUnit.SECONDS)) {
                        logError(Level.INFO, "Update check timed out");
                    }
                    dfc.pluginUpdateCheckComplete(pluginUpdates, force);
                } catch (Exception ignored) {
                    assert true;
                }

            }
        }, "Plugin update checker");
    }

    public boolean updateChecksGloballyDisabled() {
        return ENV_FB_NO_UPDATE_CHECKS || getPluginThatDisabledUpdateChecks() != null;
    }

    public String getPluginThatDisabledUpdateChecks() {
        String disable = dfc.getGlobalOption(KEY_DISABLE_ALL_UPDATE_CHECKS);
        Plugin setter = dfc.getGlobalOptionSetter(KEY_DISABLE_ALL_UPDATE_CHECKS);
        String pluginName = setter == null ? "<unknown plugin>" : setter.getShortDescription();
        String disablingPlugin = null;
        if ("true".equalsIgnoreCase(disable)) {
            logError(Level.INFO, "Skipping update checks due to " + KEY_DISABLE_ALL_UPDATE_CHECKS + "=true set by "
                    + pluginName);
            disablingPlugin = pluginName;
        } else if (disable != null && !"false".equalsIgnoreCase(disable)) {
            String error = "Unknown value '" + disable + "' for " + KEY_DISABLE_ALL_UPDATE_CHECKS + " in " + pluginName;
            logError(Level.SEVERE, error);
            throw new IllegalStateException(error);
        }
        return disablingPlugin;
    }

    private void startUpdateCheckThread(final URI url, final Collection<Plugin> plugins, final CountDownLatch latch) {
        if (url == null) {
            logError(Level.INFO, "Not checking for plugin updates w/ blank URL: " + getPluginNames(plugins));
            return;
        }
        final String entryPoint = getEntryPoint();
        if ((entryPoint.contains("edu.umd.cs.findbugs.FindBugsTestCase")
                || entryPoint.contains("edu.umd.cs.findbugs.cloud.appEngine.AbstractWebCloudTest"))
                && ("http".equals(url.getScheme()) || "https".equals(url.getScheme()))) {
            LOGGER.fine("Skipping update check because we're running in FindBugsTestCase and using "
                    + url.getScheme());
            return;
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    actuallyCheckforUpdates(url, plugins, entryPoint);
                } catch (Exception e) {
                    if (e instanceof IllegalStateException && e.getMessage().contains("Shutdown in progress")) {
                        return;
                    }
                    logError(e, "Error doing update check at " + url);
                } finally {
                    latch.countDown();
                }
            }
        };
        if (DEBUG) {
            r.run();
        } else {
            Util.runInDameonThread(r, "Check for updates");
        }
    }

    static final boolean DEBUG = SystemProperties.getBoolean("findbugs.updatecheck.debug");
    /** protected for testing */
    protected void actuallyCheckforUpdates(URI url, Collection<Plugin> plugins, String entryPoint) throws IOException {
        LOGGER.fine("Checking for updates at " + url + " for " + getPluginNames(plugins));
        if (DEBUG) {
            System.out.println(url);
        }
        HttpURLConnection conn = (HttpURLConnection) url.toURL().openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.connect();
        OutputStream out = conn.getOutputStream();
        writeXml(out, plugins, entryPoint, true);
        // for debugging:
        if (DEBUG) {
            System.out.println("Sending");
            writeXml(System.out, plugins, entryPoint, false);
        }
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            logError(SystemProperties.ASSERTIONS_ENABLED ? Level.WARNING : Level.FINE,
                    "Error checking for updates at " + url + ": "
                            + responseCode + " - " + conn.getResponseMessage());
        } else {
            parseUpdateXml(url, plugins, conn.getInputStream());
        }
        conn.disconnect();
    }

    /** protected for testing */
    @SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION")
    protected final void writeXml(OutputStream out, Collection<Plugin> plugins, String entryPoint,
            boolean finish) throws IOException {
        OutputStreamXMLOutput xmlOutput = new OutputStreamXMLOutput(out);
        try {
            xmlOutput.beginDocument();

            xmlOutput.startTag("findbugs-invocation");
            xmlOutput.addAttribute("version", Version.RELEASE);
            String applicationName = Version.getApplicationName();
            if (applicationName == null || "".equals(applicationName)) {
                int lastDot = entryPoint.lastIndexOf('.');
                if (lastDot == -1) {
                    applicationName = entryPoint;
                } else {
                    applicationName = entryPoint.substring(lastDot + 1);
                }
            }
            xmlOutput.addAttribute("app-name", applicationName);
            String applicationVersion = Version.getApplicationVersion();
            if (applicationVersion == null) {
                applicationVersion = "";
            }
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
                Date date = plugin.getReleaseDate();
                if (date != null) {
                    xmlOutput.addAttribute("release-date", Long.toString(date.getTime()));
                }
                xmlOutput.stopTag(true);
            }

            xmlOutput.closeTag("findbugs-invocation");
            xmlOutput.flush();
        } finally {
            if (finish) {
                xmlOutput.finish();
            }
        }
    }

    // package-private for testing
    @SuppressWarnings({ "unchecked" })
    void parseUpdateXml(URI url, Collection<Plugin> plugins, @WillClose
            InputStream inputStream) {
        try {
            Document doc = new SAXReader().read(inputStream);
            if (DEBUG) {
                StringWriter stringWriter = new StringWriter();
                XMLWriter xmlWriter = new XMLWriter(stringWriter);
                xmlWriter.write(doc);
                xmlWriter.close();
                System.out.println("UPDATE RESPONSE: " + stringWriter.toString());
            }
            List<Element> pluginEls =  XMLUtil.selectNodes(doc, "fb-plugin-updates/plugin");
            Map<String, Plugin> map = new HashMap<String, Plugin>();
            for (Plugin p : plugins) {
                map.put(p.getPluginId(), p);
            }
            for (Element pluginEl : pluginEls) {
                String id = pluginEl.attributeValue("id");
                Plugin plugin = map.get(id);
                if (plugin != null) {
                    checkPlugin(pluginEl, plugin);
                }

            }
        } catch (Exception e) {
            logError(e, "Could not parse plugin version update for " + url);
        } finally {
            Util.closeSilently(inputStream);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void checkPlugin(Element pluginEl, Plugin plugin) {
        for (Element release : (List<Element>) pluginEl.elements("release")) {
            checkPluginRelease(plugin, release);
        }
    }

    private void checkPluginRelease(Plugin plugin, Element maxEl) {
        @CheckForNull Date updateDate = parseReleaseDate(maxEl);
        @CheckForNull Date installedDate = plugin.getReleaseDate();
        if (updateDate != null && installedDate != null && updateDate.before(installedDate)) {
            return;
        }
        String version = maxEl.attributeValue("version");
        if (version.equals(plugin.getVersion())) {
            return;
        }

        String url = maxEl.attributeValue("url");
        String message = maxEl.element("message").getTextTrim();

        pluginUpdates.add(new PluginUpdate(plugin, version, updateDate, url, message));
    }

    // protected for testing
    protected void logError(Level level, String msg) {
        LOGGER.log(level, msg);
    }

    // protected for testing
    protected void logError(Exception e, String msg) {
        LOGGER.log(Level.INFO, msg, e);
    }

    private @CheckForNull Date parseReleaseDate(Element releaseEl) {
        SimpleDateFormat format = new SimpleDateFormat(PLUGIN_RELEASE_DATE_FMT);
        String dateStr = releaseEl.attributeValue("date");
        if (dateStr == null) {
            return null;
        }
        try {
            return format.parse(dateStr);
        } catch (java.text.ParseException e) {
            throw new IllegalArgumentException("Error parsing " + dateStr + " using " + PLUGIN_RELEASE_DATE_FMT, e);
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

    /** Should only be used once */
    private static Random random = new Random();

    private static synchronized String getUuid() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(UpdateChecker.class);
            long uuid = prefs.getLong("uuid", 0);
            if (uuid == 0) {
                uuid = random.nextLong();
                prefs.putLong("uuid", uuid);
            }
            return Long.toString(uuid, 16);
        } catch (Throwable e) {
            return Long.toString(42, 16);
        }
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
        private final @CheckForNull Date date;
        private final @CheckForNull String url;
        private final @Nonnull String message;

        private PluginUpdate(Plugin plugin, String version, @CheckForNull  Date date, @CheckForNull String url, @Nonnull String message) {
            this.plugin = plugin;
            this.version = version;
            this.date = date;
            this.url = url;
            this.message = message;
        }

        public Plugin getPlugin() {
            return plugin;
        }

        public String getVersion() {
            return version;
        }

        public @CheckForNull Date getDate() {
            return date;
        }

        public @CheckForNull String getUrl() {
            return url;
        }

        public @Nonnull String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            SimpleDateFormat format = new SimpleDateFormat(PLUGIN_RELEASE_DATE_FMT);
            StringBuilder buf = new StringBuilder();
            String name = getPlugin().isCorePlugin() ? "FindBugs" : "FindBugs plugin " + getPlugin().getShortDescription();
            buf.append( name + " " + getVersion() );
            if (date == null) {
                buf.append(" has been released");
            } else {
                buf.append(" was released " + format.format(date));
            }
            buf.append(
                    " (you have " + getPlugin().getVersion()
                    + ")");
            buf.append("\n");

            buf.append("   " + message.replaceAll("\n", "\n   "));

            if (url != null) {
                buf.append("\nVisit " + url + " for details.");
            }
            return buf.toString();
        }
    }

    public static void main(String args[]) throws Exception {
        FindBugs.setNoAnalysis();
        DetectorFactoryCollection dfc = DetectorFactoryCollection.instance();
        UpdateChecker checker = dfc.getUpdateChecker();
        if (checker.updateChecksGloballyDisabled()) {
            System.out.println("Update checkes are globally disabled");
        }
        URI redirect = checker.getRedirectURL(false);
        if (redirect != null) {
            System.out.println("All update checks redirected to " + redirect);
        }
        checker.writeXml(System.out, dfc.plugins(), "UpdateChecker", true);


    }
}
