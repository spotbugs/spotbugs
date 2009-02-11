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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * 
 */
public class LaunchBrowser {

	private static final @CheckForNull Method jnlpShowMethod;
	private static final Object jnlpShowObject; // will not be null if jnlpShowMethod!=null
	
	private static Object desktopObject;
	private static Method desktopBrowseMethod;
	static {
		try {
			Class <?> desktopClass = Class.forName("java.awt.Desktop");
			desktopObject = desktopClass.getMethod("getDesktop").invoke(null);
			desktopBrowseMethod = desktopClass.getMethod("browse", URI.class);
		}  catch (Exception e) {
			assert true;
		} 
		// attempt to set the JNLP BasicService object and its showDocument(URL) method
		Method showMethod = null;
		Object showObject = null;
		try {
			Class<?> serviceManagerClass = Class.forName("javax.jnlp.ServiceManager");
			Method lookupMethod = serviceManagerClass.getMethod("lookup", new Class[] { String.class });
			showObject = lookupMethod.invoke(null, new Object[] { "javax.jnlp.BasicService" });
			showMethod = showObject.getClass().getMethod("showDocument", new Class [] { URL.class });
		} catch (ClassNotFoundException e) {
			assert true;
		} catch (NoSuchMethodException e) {
			assert true;
		} catch (IllegalAccessException e) {
			assert true;
		} catch (InvocationTargetException e) {
			assert true;
		}
		jnlpShowMethod = showMethod;
		jnlpShowObject = showObject;
	}



	/** 
	 * attempt to show the given URL.
	 * will first attempt via the JNLP api, then will try showViaExec().
	 * @param url the URL
	 * @return true on success
	 */
	public static boolean showDocument(URL url) {
		
		if (desktopObject != null && desktopBrowseMethod != null) try { 
			return (Boolean) desktopBrowseMethod.invoke(desktopObject, url.toURI());
		} catch (InvocationTargetException ite) {
			assert true;
		} catch (IllegalAccessException iae) {
			assert true;
		} catch (IllegalArgumentException e) {
			assert true;
        } catch (URISyntaxException e) {
        	assert true;
        }
		if (jnlpShowMethod != null) try {
			Object result = jnlpShowMethod.invoke(jnlpShowObject,  url );
			return (Boolean.TRUE.equals(result));
		} catch (InvocationTargetException ite) {
			assert true;
		} catch (IllegalAccessException iae) {
			assert true;
		}
		return false;
	}


	

	
}
