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
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

import javax.annotation.CheckForNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Version number and release date information.
 */
public class Version {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    /**
     * SpotBugs website.
     */
    public static final String WEBSITE = "https://spotbugs.github.io/";

    public static final String VERSION_STRING;

    /**
     * @deprecated Use {@link #VERSION_STRING} instead.
    */
    @Deprecated
    public static final String RELEASE;

    private static String applicationName = "";
    private static String applicationVersion = "";

    static {
        final URL u = Version.class.getResource(Version.class.getSimpleName() + ".class");
        final boolean fromFile = "file".equals(u.getProtocol());

        String version = "(Unknown)";
        if (!fromFile) {
            try {
                final Enumeration<URL> resources = Version.class.getClassLoader()
                        .getResources("META-INF/MANIFEST.MF");
                while (resources.hasMoreElements()) {
                    try (final InputStream is = resources.nextElement().openStream()) {
                        final Manifest manifest = new Manifest(is);

                        // is this the one we are looking for?
                        String mainClass = manifest.getMainAttributes().getValue("Main-Class");
                        if (LaunchAppropriateUI.class.getName().equals(mainClass)) {
                            version = manifest.getMainAttributes().getValue("Bundle-Version");
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                // ignore it
            }
        } else {
            version = "Development";
        }

        RELEASE = VERSION_STRING = version;
    }

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

    public static void main(String[] argv) {
        if (argv.length == 0) {
            printVersion(false);
            return;
        }

        String arg = argv[0];

        if ("-release".equals(arg)) {
            LOG.info(VERSION_STRING);
        } else if ("-plugins".equals(arg)) {
            DetectorFactoryCollection.instance();
            for (Plugin p : Plugin.getAllPlugins()) {
                LOG.info("Plugin: {}", p.getPluginId());
                LOG.info("  description: {}", p.getShortDescription());
                LOG.info("     provider: {}", p.getProvider());
                String version = p.getVersion();
                if (version != null && version.length() > 0) {
                    LOG.info("      version: {}", version);
                }
                String website = p.getWebsite();
                if (website != null && website.length() > 0) {
                    LOG.info("      website: {}", website);
                }
                LOG.info("");
            }
        } else if ("-configuration".equals(arg)) {
            printVersion(true);
        } else {
            usage();
            System.exit(1);
        }
    }

    private static void usage() {
        LOG.error("Usage: {} [(-release|-date|-props|-configuration)]", Version.class.getName());
    }

    /**
     * @param justPrintConfiguration
     */
    public static void printVersion(boolean justPrintConfiguration) {
        LOG.info("SpotBugs {}", Version.VERSION_STRING);
        if (!justPrintConfiguration) {
            return;
        }
        for (Plugin plugin : Plugin.getAllPlugins()) {
            LOG.info("Plugin {}, version {}, loaded from {}",
                    plugin.getPluginId(), plugin.getVersion(), plugin.getPluginLoader().getURL());
            if (plugin.isCorePlugin()) {
                LOG.info("  is core plugin");
            }
            if (plugin.isInitialPlugin()) {
                LOG.info("  is initial plugin");
            }
            if (plugin.isEnabledByDefault()) {
                LOG.info("  is enabled by default");
            }
            if (plugin.isGloballyEnabled()) {
                LOG.info("  is globally enabled");
            }
            Plugin parent = plugin.getParentPlugin();
            if (parent != null) {
                LOG.info("  has parent plugin {}", parent.getPluginId());
            }

            for (DetectorFactory factory : plugin.getDetectorFactories()) {
                LOG.info("  detector {}", factory.getShortName());
            }
            LOG.info("");
        }
    }
}
