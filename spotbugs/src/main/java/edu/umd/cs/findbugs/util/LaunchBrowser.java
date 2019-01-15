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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import edu.umd.cs.findbugs.SystemProperties;

/**
 *
 */
public class LaunchBrowser {

    private static Pattern validExec = Pattern.compile("[a-zA-Z0-9-_/]+");

    public static final String execCommand = SystemProperties.getProperty("findbugs.execCommand");

    public static final boolean launchViaExec = execCommand != null && validExec.matcher(execCommand).matches()
            && "Linux".equals(SystemProperties.getProperty("os.name"));

    private static Object desktopObject;

    private static Method desktopBrowseMethod;

    private static boolean launchViaExecFailed = false;

    static boolean DEBUG = SystemProperties.getBoolean("findbugs.launchBrowser.debug");

    static final Exception desktopException;

    static {
        Exception toStore = null;
        try {
            Class<?> desktopClass = Class.forName("java.awt.Desktop");
            desktopObject = desktopClass.getMethod("getDesktop").invoke(null);
            desktopBrowseMethod = desktopClass.getMethod("browse", URI.class);
        } catch (Exception e) {
            toStore = e;
        }
        desktopException = toStore;
    }

    static boolean desktopFeasible() {
        return desktopObject != null && desktopBrowseMethod != null;
    }

    static boolean webstartFeasible() {
        return JavaWebStart.jnlpShowDocumentMethod != null && JavaWebStart.jnlpBasicService != null;
    }

    static boolean showDocumentViaDesktop(URL u) {

        if (desktopObject != null && desktopBrowseMethod != null) {
            try {
                if (DEBUG) {
                    JOptionPane.showMessageDialog(null, "Trying desktop browse");
                }
                viaDesktop(u.toURI());
                if (DEBUG) {
                    JOptionPane.showMessageDialog(null, "desktop browse succeeded");
                }
                return true;
            } catch (InvocationTargetException ite) {
                assert true;
            } catch (IllegalAccessException iae) {
                assert true;
            } catch (IllegalArgumentException e) {
                assert true;
            } catch (URISyntaxException e) {
                assert true;
            }
        }
        return false;
    }

    static void viaDesktop(URI u) throws IllegalAccessException, InvocationTargetException {
        if (desktopBrowseMethod == null) {
            throw new UnsupportedOperationException("Launch via desktop not available");
        }
        desktopBrowseMethod.invoke(desktopObject, u);
    }

    static boolean showDocumentViaExec(URL url) {
        if (launchViaExec && !launchViaExecFailed) {
            if (DEBUG) {
                JOptionPane.showMessageDialog(null, "Trying exec browse");
            }

            try {
                Process p = launchViaExec(url);
                Thread.sleep(90);

                int exitValue = p.exitValue();
                if (exitValue != 0) {
                    launchViaExecFailed = true;
                    if (DEBUG) {
                        JOptionPane.showMessageDialog(null, "exec browse launch failed with exit code " + exitValue);
                    }
                    return false;
                }
                if (DEBUG) {
                    JOptionPane.showMessageDialog(null, "exec browse succeeded");
                }
                return true;
            } catch (IllegalThreadStateException e) {
                if (DEBUG) {
                    JOptionPane.showMessageDialog(null, "exec browse succeeded but not done");
                }
                return true;
            } catch (Exception e) {
                if (DEBUG) {
                    JOptionPane.showMessageDialog(null, "exec browse failed" + e.getMessage());
                }
                launchViaExecFailed = true;
            }
        }
        return false;

    }

    static Process launchViaExec(URL url) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(execCommand, url.toString());
        Process p = builder.start();
        return p;
    }

    /**
     * attempt to show the given URL. will first attempt via the JNLP api, then
     * will try showViaExec().
     *
     * @param url
     *            the URL
     * @return true on success
     */
    public static boolean showDocument(URL url) {

        if (showDocumentViaDesktop(url)) {
            return true;
        }
        if (showDocumentViaExec(url)) {
            return true;
        }
        if (JavaWebStart.showViaWebStart(url)) {
            return true;
        }

        return false;

    }

}
