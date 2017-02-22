/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2010 University of Maryland
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

package edu.umd.cs.findbugs.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.CheckForNull;
import javax.swing.JOptionPane;


/**
 * @author pugh
 */
public class JavaWebStart {

    static final @CheckForNull
    Method jnlpShowDocumentMethod;

    static final @CheckForNull
    Method jnlpGetCodeBaseMethod;

    static final Object jnlpBasicService; // will not be null if
    // jnlpShowMethod!=null

    static {
        // attempt to set the JNLP BasicService object and its showDocument(URL)
        // method
        Method showMethod = null;
        Method getCodeBase = null;

        Object showObject = null;
        try {
            Class<?> serviceManagerClass = Class.forName("javax.jnlp.ServiceManager");
            Method lookupMethod = serviceManagerClass.getMethod("lookup", new Class[] { String.class });
            showObject = lookupMethod.invoke(null, new Object[] { "javax.jnlp.BasicService" });
            showMethod = showObject.getClass().getMethod("showDocument", new Class[] { URL.class });
            getCodeBase = showObject.getClass().getMethod("getCodeBase", new Class[] {});

        } catch (ClassNotFoundException e) {
            assert true;
        } catch (NoSuchMethodException e) {
            assert true;
        } catch (IllegalAccessException e) {
            assert true;
        } catch (InvocationTargetException e) {
            assert true;
        }
        jnlpShowDocumentMethod = showMethod;
        jnlpGetCodeBaseMethod = getCodeBase;
        jnlpBasicService = showObject;

    }

    public static boolean isRunningViaJavaWebstart() {
        return JavaWebStart.jnlpBasicService != null;
    }

    public static URL resolveRelativeToJnlpCodebase(String s) throws MalformedURLException {
        if (JavaWebStart.jnlpGetCodeBaseMethod != null) {
            try {
                URL base = (URL) JavaWebStart.jnlpGetCodeBaseMethod.invoke(JavaWebStart.jnlpBasicService);
                if (base != null) {
                    return new URL(base, s);
                }
            } catch (RuntimeException e) {
                assert true;
            } catch (Exception e) {
                assert true;
            }
        }
        return new URL(s);
    }

    static Boolean viaWebStart(URL url) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        if (JavaWebStart.jnlpShowDocumentMethod == null) {
            throw new UnsupportedOperationException("Launch via web start not available");
        }
        return (Boolean) JavaWebStart.jnlpShowDocumentMethod.invoke(JavaWebStart.jnlpBasicService, url);
    }

    static boolean showViaWebStart(URL url) {
        if (JavaWebStart.jnlpShowDocumentMethod != null) {
            try {
                if (LaunchBrowser.DEBUG) {
                    JOptionPane.showMessageDialog(null, "Trying browse via webstart");
                }

                Boolean b = viaWebStart(url);
                boolean success = b != null && b.booleanValue();

                if (LaunchBrowser.DEBUG) {
                    JOptionPane.showMessageDialog(null, " browse via webstart: " + success);
                }
                return success;

            } catch (InvocationTargetException ite) {
                assert true;
            } catch (IllegalAccessException iae) {
                assert true;
            }
        }
        return false;
    }

}
