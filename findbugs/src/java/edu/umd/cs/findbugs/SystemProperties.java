/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.IllegalFormatException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.io.IO;

/**
 * @author pugh
 */
public class SystemProperties {

    private static Properties properties = new Properties();

    public final static boolean ASSERTIONS_ENABLED;

    public final static boolean RUNNING_IN_ECLIPSE = SystemProperties.class.getClassLoader().getClass().getCanonicalName()
            .startsWith("org.eclipse.osgi");

    final static String OS_NAME;
    static {
        boolean tmp = false;
        assert tmp = true; // set tmp to true if assertions are enabled
        ASSERTIONS_ENABLED = tmp;
        String osName;
        try {
            osName = "." + System.getProperty("os.name", "Unknown").replace(' ', '_');
        } catch (Throwable e) {
            osName = ".Unknown";
        }
        OS_NAME = osName;
        loadPropertiesFromConfigFile();
        if (getBoolean("findbugs.dumpProperties")) {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream("/tmp/outProperties.txt");
                System.getProperties().store(out, "System properties dump");
                properties.store(out, "FindBugs properties dump");
            } catch (IOException e) {
                assert true;
            } finally {
                IO.close(out);
            }
        }
    }

    private static void loadPropertiesFromConfigFile() {

        URL systemProperties = DetectorFactoryCollection.getCoreResource("systemProperties.properties");
        loadPropertiesFromURL(systemProperties);
        String u = System.getProperty("findbugs.loadPropertiesFrom");
        if (u != null) {
            try {
                URL configURL = new URL(u);
                loadPropertiesFromURL(configURL);
            } catch (MalformedURLException e) {
                AnalysisContext.logError("Unable to load properties from " + u, e);

            }
        }
    }

    public static Properties getLocalProperties() {
        return properties;
    }

    public static Properties getAllProperties() {
        Properties result = System.getProperties();
        result.putAll(properties);
        return result;
    }

    /**
     * This method is public to allow clients to set system properties via any
     * {@link URL}
     *
     * @param url
     *            an url to load system properties from, may be nullerrorMsg
     */
    public static void loadPropertiesFromURL(URL url) {
        if (url == null) {
            return;
        }
        InputStream in = null;
        try {
            in = url.openStream();
            properties.load(in);
        } catch (IOException e) {
            AnalysisContext.logError("Unable to load properties from " + url, e);
        } finally {
            IO.close(in);
        }
    }

    /**
     * Get boolean property, returning false if a security manager prevents us
     * from accessing system properties
     * <p>
     * (incomplete) list of known system properties
     * <ul>
     * <li>
     * "report_TESTING_pattern_in_standard_detectors" - default is false
     * </li>
     * </ul>
     *
     * @return true if the property exists and is set to true
     */
    public static boolean getBoolean(String name) {
        return getBoolean(name, false);
    }

    public static boolean getBoolean(String name, boolean defaultValue) {
        boolean result = defaultValue;
        try {
            String value = getProperty(name);
            if (value == null) {
                return defaultValue;
            }
            result = toBoolean(value);
        } catch (IllegalArgumentException e) {
        } catch (NullPointerException e) {
        }
        return result;
    }

    private static boolean toBoolean(String name) {
        return ((name != null) && "true".equalsIgnoreCase(name));
    }

    /**
     * @param arg0
     *            property name
     * @param arg1
     *            default value
     * @return the int value (or arg1 if the property does not exist)
     * @deprecated Use {@link #getInt(String,int)} instead
     */
    @Deprecated
    public static Integer getInteger(String arg0, int arg1) {
        return getInt(arg0, arg1);
    }

    /**
     * @param name
     *            property name
     * @param defaultValue
     *            default value
     * @return the int value (or defaultValue if the property does not exist)
     */
    public static int getInt(String name, int defaultValue) {
        try {
            String value = getProperty(name);
            if (value != null) {
                return Integer.decode(value);
            }
        } catch (Exception e) {
            assert true;
        }
        return defaultValue;
    }

    /**
     * @param name
     *            property name
     * @return string value (or null if the property does not exist)
     */
    public static String getOSDependentProperty(String name) {
        String osDependentName = name + OS_NAME;
        String value = getProperty(osDependentName);
        if (value != null) {
            return value;
        }
        return getProperty(name);
    }

    /**
     * @param name
     *            property name
     * @return string value (or null if the property does not exist)
     */
    public static String getProperty(String name) {
        try {
            String value = properties.getProperty(name);
            if (value != null) {
                return value;
            }
            return System.getProperty(name);
        } catch (Exception e) {
            return null;
        }

    }

    public static void setProperty(String name, String value) {
        properties.setProperty(name, value);
    }

    /**
     * @param name
     *            property name
     * @param defaultValue
     *            default value
     * @return string value (or defaultValue if the property does not exist)
     */
    public static String getProperty(String name, String defaultValue) {
        try {
            String value = properties.getProperty(name);
            if (value != null) {
                return value;
            }
            return System.getProperty(name, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static final String URL_REWRITE_PATTERN_STRING = getOSDependentProperty("findbugs.urlRewritePattern");

    private static final String URL_REWRITE_FORMAT = getOSDependentProperty("findbugs.urlRewriteFormat");

    private static final Pattern URL_REWRITE_PATTERN;

    static {
        Pattern p = null;
        if (URL_REWRITE_PATTERN_STRING != null && URL_REWRITE_FORMAT != null) {
            try {
                p = Pattern.compile(URL_REWRITE_PATTERN_STRING);
                String ignored = String.format(URL_REWRITE_FORMAT, "");
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Bad findbugs.urlRewritePattern '" + URL_REWRITE_PATTERN_STRING + "' - "
                        + e.getClass().getSimpleName() + ": "+ e.getMessage());
            } catch (IllegalFormatException e) {
                throw new IllegalArgumentException("Bad findbugs.urlRewriteFormat '" + URL_REWRITE_FORMAT + "' - "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        } else if (URL_REWRITE_PATTERN_STRING != null) {
            throw new IllegalArgumentException("findbugs.urlRewritePattern is set but not findbugs.urlRewriteFormat");
        } else if (URL_REWRITE_FORMAT != null) {
            throw new IllegalArgumentException("findbugs.urlRewriteFormat is set but not findbugs.urlRewritePattern");
        }
        URL_REWRITE_PATTERN = p;
    }

    public static String rewriteURLAccordingToProperties(String u) {
        if (URL_REWRITE_PATTERN == null || URL_REWRITE_FORMAT == null) {
            return u;
        }
        Matcher m = URL_REWRITE_PATTERN.matcher(u);
        if (!m.matches() || m.groupCount() == 0) {
            return u;
        }
        String result = String.format(URL_REWRITE_FORMAT, m.group(1));
        return result;
    }

}
