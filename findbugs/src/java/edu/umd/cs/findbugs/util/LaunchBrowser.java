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
import java.net.URL;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * 
 */
public class LaunchBrowser {

	private static final @CheckForNull Method jnlpShowMethod;
	private static final Object jnlpShowObject; // will not be null if jnlpShowMethod!=null
	static {
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

		if (jnlpShowMethod != null) try {
			Object result = jnlpShowMethod.invoke(jnlpShowObject, new Object [] { url });
			return (Boolean.TRUE.equals(result));
		} catch (InvocationTargetException ite) {
			// do nothing
		} catch (IllegalAccessException iae) {
			// do nothing
		}

		// fallback to exec()
		return showViaExec( url.toString() ); // or url.toExternalForm()
	}


	/** attempt to show the given URL string.
	 *  will first attempt via the JNLP api, then will try showViaExec().
	 * @param url the url srtring
	 * @return true on success
	 */
	public static boolean showDocument(String url) {

		if (jnlpShowMethod != null) {
			try {
				new URL(url);
			} catch (MalformedURLException mue) {
				return false;
			}
			try {
				Object result = jnlpShowMethod.invoke(jnlpShowObject, new Object[] { url });
				System.out.println("jnlp result is " + result);
				return (Boolean.TRUE.equals(result));
			} catch (InvocationTargetException ite) {
				assert true; // do nothing
			} catch (IllegalAccessException iae) {
				assert true; // do nothing
			}
		}
		// fallback to exec()
		return showViaExec(url);
	}


	/**
	 * DISABLED -- simply returns false
	 * 
	 * Attempts to show the given URL in the OS's web browser.
	 * @param url url to show
	 * @return true if the show operation was successful, false otherwise.
	 */
	private static boolean showViaExec(String url){
		return false;
		/*
		String os = SystemProperties.getProperty("os.name").toLowerCase();
		Runtime rt = Runtime.getRuntime();
		try{
			if (os.indexOf( "win" ) >= 0) {
				// this doesn't support showing urls in the form of "page.html#nameLink" 
				rt.exec( "rundll32 url.dll,FileProtocolHandler " + url);
			} else if (os.indexOf( "mac" ) >= 0) {
				rt.exec( "open " + url);
			} else if (os.indexOf( "nix") >=0 || os.indexOf( "nux") >=0) {
				// Do a best guess on unix until we get a platform independent way
				// Build a list of browsers to try, in this order.
				String[] browsers = {"epiphany", "firefox", "mozilla", "konqueror",
						"netscape","opera","links","lynx"};

				// Build a command string which looks like "browser1 "url" || browser2 "url" ||..."
				StringBuffer cmd = new StringBuffer();
				for (int i=0; i<browsers.length; i++)
					cmd.append( (i==0  ? "" : " || " ) + browsers[i] +" \"" + url + "\" ");

				rt.exec(new String[] { "sh", "-c", cmd.toString() });
			} else {
				return false;
			}
		}catch (IOException e){
			return false;
		}
		return true;
		*/
	}

}
