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
import java.util.Properties;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.io.IO;

/**
 * @author pugh
 */
public class SystemProperties {

	private static Properties properties = new Properties(System.getProperties());
	public final static boolean ASSERTIONS_ENABLED;
	static {
		boolean tmp = false;
		assert tmp = true; // set tmp to true if assertions are enabled
		ASSERTIONS_ENABLED = tmp;
		loadPropertiesFromConfigFile();
		if (getBoolean("findbugs.dumpProperties")){
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
	
	private static void loadPropertiesFromConfigFile()  {
		
	    URL systemProperties = PluginLoader.getCoreResource("systemProperties.txt");
		loadPropertiesFromURL(systemProperties);
		String u = System.getProperty("findbugs.loadPropertiesFrom");
		if (u != null)
			try {
			 URL configURL = new URL(u);
			 loadPropertiesFromURL(configURL);
			} catch (MalformedURLException e) {
	            AnalysisContext.logError("Unable to load properties from " + u, e);
	            
            }
		}
	
	/**
	 * This method is public to allow clients to set system properties via any {@link URL}
	 * 
     * @param url an url to load system properties from, may be null
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
	 * @return true if the property exists and is set to true
	 */
	public static boolean getBoolean(String name) {
		return getBoolean(name, false);
	}

	public static boolean getBoolean(String name, boolean defaultValue) {
		boolean result = defaultValue;
		try {
			String value = getProperty(name);
			if (value == null) return defaultValue;
			result = toBoolean(value);
		} catch (IllegalArgumentException e) {
		} catch (NullPointerException e) {
		}
		return result;
	}
	private static boolean toBoolean(String name) {
		return ((name != null) && name.equalsIgnoreCase("true"));
	}


	/**
     * @param arg0 property name
     * @param arg1 default value
     * @return the int value (or arg1 if the property does not exist)
     * @deprecated Use {@link #getInt(String,int)} instead
     */
    public static Integer getInteger(String arg0, int arg1) {
        return getInt(arg0, arg1);
    }
	/**
	 * @param name property name
	 * @param defaultValue default value
	 * @return the int value (or defaultValue if the property does not exist)
	 */
	public static int getInt(String name, int defaultValue) {
		try {
			String value = getProperty(name);
			if (value != null) 
				return Integer.decode(value);
		} catch (Exception e) {
			assert true;
		}
		return defaultValue;
	}

	/**
	 * @param name property name
	 * @return string value (or null if the property does not exist)
	 */
	public static String getProperty(String name) {
		try {
			return properties.getProperty(name);
		} catch (Exception e) {
			assert true;
		}
		return null;
	}

	/**
	 * @param name property name
	 * @param defaultValue default value
	 * @return string value (or defaultValue if the property does not exist)
	 */
	public static String getProperty(String name, String defaultValue) {
		try {
		return properties.getProperty(name, defaultValue);
		} catch (Exception e) {
			return defaultValue;
		}
	}

}
