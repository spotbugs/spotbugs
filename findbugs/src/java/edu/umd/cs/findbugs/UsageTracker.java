package edu.umd.cs.findbugs;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

public class UsageTracker {
    private static final Logger LOGGER = Logger.getLogger(UsageTracker.class.getName());
    
    public void trackUsage(final URI trackerUrl, final Set<Plugin> plugins) {
        if (trackerUrl == null) {
            LOGGER.info("Not submitting usage tracking for plugins with blank URL: " + getPluginNames(plugins));
            return;
        }
        final String entryPoint = getEntryPoint();
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    actuallyTrackUsage(trackerUrl, plugins, entryPoint);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error submitting usage tracking data to " + trackerUrl, e);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void actuallyTrackUsage(URI trackerUrl, Set<Plugin> plugins, String entryPoint) throws IOException {
        if (false) 
        System.out.println("Submitting anonymous usage tracking info to " + trackerUrl + " for " + getPluginNames(plugins));
        HttpURLConnection conn = (HttpURLConnection) trackerUrl.toURL().openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.connect();
        OutputStream out = conn.getOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(out);
        xmlOutput.beginDocument();
        xmlOutput.startTag("findbugs-invocation");
        xmlOutput.addAttribute("version", Version.RELEASE);
        xmlOutput.addAttribute("app-name", Version.getApplicationName());
        xmlOutput.addAttribute("app-version", Version.getApplicationVersion());
        xmlOutput.addAttribute("entry-point", entryPoint);
        xmlOutput.addAttribute("os", SystemProperties.getProperty("os.name", ""));
        xmlOutput.addAttribute("java-version", getMajorJavaVersion());
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
        xmlOutput.finish();
        out.close();
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            LOGGER.warning("Error while submitting anonymous usage data: "
                    + responseCode + " - " + conn.getResponseMessage());
        }
    }

    private String getPluginNames(Set<Plugin> plugins) {
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
}
