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
import java.util.Enumeration;
import java.util.jar.Manifest;

import javax.annotation.CheckForNull;

/**
 * Version number and release date information.
 */
public class Version {
    /**
     * SpotBugs website.
     */
    public static final String WEBSITE = "https://spotbugs.github.io/";
    
    public final static String VERSION_STRING;

    /**
     * @deprecated Use {@link #VERSION_STRING} instead.
    */
    @Deprecated
    public final static String RELEASE;
    
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

    public static void main(String[] argv) throws InterruptedException {
        if (argv.length == 0) {
            printVersion(false);
            return;
        }

        String arg = argv[0];

        if ("-release".equals(arg)) {
            System.out.println(VERSION_STRING);
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

    /**
     * @param justPrintConfiguration
     * @throws InterruptedException
     */
    public static void printVersion(boolean justPrintConfiguration) throws InterruptedException {
        System.out.println("SpotBugs " + Version.VERSION_STRING);
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

                for (DetectorFactory factory : plugin.getDetectorFactories()) {
                    System.out.printf("  detector %s%n", factory.getShortName());
                }
                System.out.println();
            }
        }
    }
}

