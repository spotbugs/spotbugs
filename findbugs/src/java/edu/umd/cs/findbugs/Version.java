/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs;

import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.updates.UpdateChecker;
import edu.umd.cs.findbugs.util.FutureValue;
import edu.umd.cs.findbugs.util.Util;

/**
 * Version number and release date information.
 */
public class Version {
    /**
     * Major version number.
     */
    public static final int MAJOR = 3;

    /**
     * Minor version number.
     */
    public static final int MINOR = 0;

    /**
     * Patch level.
     */
    public static final int PATCHLEVEL = 2;

    /**
     * Development version or release candidate?
     */
    public static final boolean IS_DEVELOPMENT = true;

    /**
     * Release candidate number. "0" indicates that the version is not a release
     * candidate.
     */
    public static final int RELEASE_CANDIDATE = 0;


    public static final String GIT_REVISION  = System.getProperty("git.revision", "UNKNOWN");

    /**
     * Release date.
     */
    private static final String COMPUTED_DATE;

    public static final String DATE;

    public static final String CORE_PLUGIN_RELEASE_DATE;

    private static final String COMPUTED_ECLIPSE_DATE;

    private static final String COMPUTED_PLUGIN_RELEASE_DATE;

    private static String applicationName = "";

    private static String applicationVersion = "";

    static {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss z, dd MMMM, yyyy", Locale.ENGLISH);
        SimpleDateFormat eclipseDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
        SimpleDateFormat releaseDateFormat = new SimpleDateFormat(UpdateChecker.PLUGIN_RELEASE_DATE_FMT, Locale.ENGLISH);
        Date now = new Date();
        COMPUTED_DATE = dateFormat.format(now);
        COMPUTED_ECLIPSE_DATE = eclipseDateFormat.format(now);
        String tmp =  releaseDateFormat.format(now);
        COMPUTED_PLUGIN_RELEASE_DATE = tmp;
    }

    /**
     * Preview release number. "0" indicates that the version is not a preview
     * release.
     */
    public static final int PREVIEW = 0;

    private static final String RELEASE_SUFFIX_WORD;
    static {
        String suffix;
        if (RELEASE_CANDIDATE > 0) {
            suffix = "rc" + RELEASE_CANDIDATE;
        } else if (PREVIEW > 0) {
            suffix = "preview" + PREVIEW;
        } else {
            suffix = "dev-" + COMPUTED_ECLIPSE_DATE;
            if (!"Unknown".equals(GIT_REVISION)) {
                suffix += "-" + GIT_REVISION;
            }
        }
        RELEASE_SUFFIX_WORD = suffix;
    }

    public static final String RELEASE_BASE = MAJOR + "." + MINOR + "." + PATCHLEVEL;

    /**
     * Release version string.
     */
    public static final String COMPUTED_RELEASE = RELEASE_BASE + (IS_DEVELOPMENT ? "-" + RELEASE_SUFFIX_WORD : "");

    /**
     * Release version string.
     */
    public static final String RELEASE;

    /**
     * Version of Eclipse plugin.
     */
    private static final String COMPUTED_ECLIPSE_UI_VERSION = RELEASE_BASE + "." + COMPUTED_ECLIPSE_DATE;

    static {
        Class<Version> c = Version.class;
        URL u = c.getResource(c.getSimpleName() + ".class");
        boolean fromFile = "file".equals(u.getProtocol());
        InputStream in = null;
        String release = null;
        String date = null;
        String plugin_release_date = null;
        if (!fromFile) {
            try {
                Properties versionProperties = new Properties();
                in = Version.class.getResourceAsStream("version.properties");
                if (in != null)  {
                    versionProperties.load(in);
                    release = (String) versionProperties.get("release.number");
                    date = (String) versionProperties.get("release.date");
                    plugin_release_date =  (String) versionProperties.get("plugin.release.date");
                }
            } catch (Exception e) {
                assert true; // ignore
            } finally {
                Util.closeSilently(in);
            }
        }
        if (release == null) {
            release = COMPUTED_RELEASE;
        }
        if (date == null) {
            date = COMPUTED_DATE;
        }
        if (plugin_release_date == null) {
            plugin_release_date = COMPUTED_PLUGIN_RELEASE_DATE;
        }

        RELEASE = release;
        DATE = date;
        CORE_PLUGIN_RELEASE_DATE = plugin_release_date;
        Date parsedDate;
        try {
            SimpleDateFormat fmt = new SimpleDateFormat(UpdateChecker.PLUGIN_RELEASE_DATE_FMT, Locale.ENGLISH);

            parsedDate = fmt.parse(CORE_PLUGIN_RELEASE_DATE);
        } catch (ParseException e) {
            if (SystemProperties.ASSERTIONS_ENABLED) {
                e.printStackTrace();
            }
            parsedDate = null;
        }
        releaseDate = parsedDate;
    }

    /**
     * FindBugs website.
     */
    public static final String WEBSITE = "http://findbugs.sourceforge.net";

    /**
     * Downloads website.
     */
    public static final String DOWNLOADS_WEBSITE = "http://prdownloads.sourceforge.net/findbugs";

    /**
     * Support email.
     */
    public static final String SUPPORT_EMAIL = "http://findbugs.sourceforge.net/reportingBugs.html";
    private static Date releaseDate;

    public static void registerApplication(String name, String version) {
        applicationName = name;
        applicationVersion = version;
    }

    public static @CheckForNull String getApplicationName() {
        return applicationName;
    }

    public static @CheckForNull String getApplicationVersion() {
        return applicationVersion;
    }

    public static void main(String[] argv) throws InterruptedException {

        if (!IS_DEVELOPMENT && RELEASE_CANDIDATE != 0) {
            throw new IllegalStateException("Non developmental version, but is release candidate " + RELEASE_CANDIDATE);
        }
        if (argv.length == 0) {
            printVersion(false);
            return;
        }

        String arg = argv[0];

        if ("-release".equals(arg)) {
            System.out.println(RELEASE);
        } else if ("-date".equals(arg)) {
            System.out.println(DATE);
        } else if ("-props".equals(arg)) {
            System.out.println("release.base=" + RELEASE_BASE);
            System.out.println("release.number=" + COMPUTED_RELEASE);
            System.out.println("release.date=" + COMPUTED_DATE);
            System.out.println("plugin.release.date=" + COMPUTED_PLUGIN_RELEASE_DATE);
            System.out.println("eclipse.ui.version=" + COMPUTED_ECLIPSE_UI_VERSION);
            System.out.println("findbugs.website=" + WEBSITE);
            System.out.println("findbugs.downloads.website=" + DOWNLOADS_WEBSITE);
            System.out.println("findbugs.git.revision=" + GIT_REVISION);
        } else if ("-plugins".equals(arg)) {
            DetectorFactoryCollection.instance();
            for(Plugin p : Plugin.getAllPlugins()) {
                System.out.println("Plugin: " + p.getPluginId());
                System.out.println("  description: " + p.getShortDescription());
                System.out.println("     provider: " + p.getProvider());
                String version = p.getVersion();
                if (version != null && version.length() > 0) {
                    System.out.println("      version: " + version);
                }
                String website = p.getWebsite();
                if (website != null && website.length() > 0) {
                    System.out.println("      website: " + website);
                }
                System.out.println();
            }
        } else if ("-configuration".equals(arg)){
            printVersion(true);
        } else {

            usage();
            System.exit(1);
        }
    }

    private static void usage() {
        System.err.println("Usage: " + Version.class.getName() + "  [(-release|-date|-props|-configuration)]");
    }

    public static String getReleaseWithDateIfDev() {
        if (IS_DEVELOPMENT) {
            return RELEASE + " (" + DATE + ")";
        }
        return RELEASE;
    }

    public static @CheckForNull Date getReleaseDate() {
        return releaseDate;
    }

    /**
     * @param justPrintConfiguration
     * @throws InterruptedException
     */
    public static void printVersion(boolean justPrintConfiguration) throws InterruptedException {
        System.out.println("FindBugs " + Version.COMPUTED_RELEASE);
        if (justPrintConfiguration) {
            for (Plugin plugin : Plugin.getAllPlugins()) {
                System.out.printf("Plugin %s, version %s, loaded from %s%n", plugin.getPluginId(), plugin.getVersion(),
                        plugin.getPluginLoader().getURL());
                if (plugin.isCorePlugin()) {
                    System.out.println("  is core plugin");
                }
                if (plugin.isInitialPlugin()) {
                    System.out.println("  is initial plugin");
                }
                if (plugin.isEnabledByDefault()) {
                    System.out.println("  is enabled by default");
                }
                if (plugin.isGloballyEnabled()) {
                    System.out.println("  is globally enabled");
                }
                Plugin parent = plugin.getParentPlugin();
                if (parent != null) {
                    System.out.println("  has parent plugin " + parent.getPluginId());
                }

                for (CloudPlugin cloudPlugin : plugin.getCloudPlugins()) {
                    System.out.printf("  cloud %s%n", cloudPlugin.getId());
                    System.out.printf("     %s%n", cloudPlugin.getDescription());
                }
                for (DetectorFactory factory : plugin.getDetectorFactories()) {
                    System.out.printf("  detector %s%n", factory.getShortName());
                }
                System.out.println();
            }
            printPluginUpdates(true, 10);
        } else {
            printPluginUpdates(false, 3);
        }
    }

    private static void printPluginUpdates(boolean verbose, int secondsToWait) throws InterruptedException {
        DetectorFactoryCollection dfc = DetectorFactoryCollection.instance();

        if (dfc.getUpdateChecker().updateChecksGloballyDisabled()) {
            if (verbose) {
                System.out.println();
                System.out.print("Update checking globally disabled");
            }
            return;
        }
        if (verbose) {
            System.out.println();
            System.out.print("Checking for plugin updates...");
        }
        FutureValue<Collection<UpdateChecker.PluginUpdate>>
        updateHolder  = dfc.getUpdates();

        try {
            Collection<UpdateChecker.PluginUpdate> updates = updateHolder.get(secondsToWait, TimeUnit.SECONDS);
            if (updates.isEmpty()) {
                if (verbose) {
                    System.out.println("none!");
                }
            } else {
                System.out.println();
                for (UpdateChecker.PluginUpdate update : updates) {
                    System.out.println(update);
                    System.out.println();

                }
            }
        } catch (TimeoutException e) {
            if (verbose) {
                System.out.println("Timeout while trying to get updates");
            }
        }

    }

}

