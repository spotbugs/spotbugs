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

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.io.IO;

/**
 * @author pugh
 */
public class PropertyBundle {

	private final Properties properties = new Properties();

	private final String urlRewritePatternString;

	private final Pattern urlRewritePattern;

	private final String urlRewriteFormat;

	public PropertyBundle() {
		urlRewritePatternString = getOSDependentProperty("findbugs.urlRewritePattern");
		urlRewriteFormat = getOSDependentProperty("findbugs.urlRewriteFormat");

		Pattern p = null;
		if (urlRewritePatternString != null && urlRewriteFormat != null)
			try {
				p = Pattern.compile(urlRewritePatternString);
			} catch (Exception e) {
				assert true;
			}
		urlRewritePattern = p;
	}

	public Properties getProperties() {
		return properties;
	}

	public void loadPropertiesFromString(String contents) {
		if (contents == null) {
			return;
		}
		InputStream in = null;
		try {
			in = new ByteArrayInputStream(contents.getBytes("ISO-8859-1"));
			properties.load(in);
		} catch (IOException e) {
			AnalysisContext.logError("Unable to load properties from " + contents, e);
		} finally {
			IO.close(in);
		}
	}

	public void loadPropertiesFromURL(URL url) {

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
	 * 
	 * @return true if the property exists and is set to true
	 */
	public boolean getBoolean(String name) {
		return getBoolean(name, false);
	}

	public boolean getBoolean(String name, boolean defaultValue) {
		boolean result = defaultValue;
		try {
			String value = getProperty(name);
			if (value == null)
				return defaultValue;
			result = toBoolean(value);
		} catch (IllegalArgumentException e) {
		} catch (NullPointerException e) {
		}
		return result;
	}

	private boolean toBoolean(String name) {
		return ((name != null) && name.equalsIgnoreCase("true"));
	}

	/**
	 * @param name
	 *            property name
	 * @param defaultValue
	 *            default value
	 * @return the int value (or defaultValue if the property does not exist)
	 */
	public int getInt(String name, int defaultValue) {
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
	 * @param name
	 *            property name
	 * @return string value (or null if the property does not exist)
	 */
	public String getOSDependentProperty(String name) {
		String osDependentName = name + SystemProperties.OS_NAME;
		String value = getProperty(osDependentName);
		if (value != null)
			return value;
		return getProperty(name);
	}

	/**
	 * @param name
	 *            property name
	 * @return string value (or null if the property does not exist)
	 */
	public String getProperty(String name) {
		try {
			String value = properties.getProperty(name);
			if (value != null)
				return value;
			return SystemProperties.getProperty(name);
		} catch (Exception e) {
			return null;
		}

	}

	/**
	 * @param name
	 *            property name
	 * @return string value (or null if the property does not exist)
	 */
	public void setProperty(String name, String value) {
		properties.setProperty(name, value);
	}

	/**
	 * @param name
	 *            property name
	 * @param defaultValue
	 *            default value
	 * @return string value (or defaultValue if the property does not exist)
	 */
	public String getProperty(String name, String defaultValue) {
		try {
			String value = properties.getProperty(name);
			if (value != null)
				return value;
			return SystemProperties.getProperty(name, defaultValue);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public String rewriteURLAccordingToProperties(String u) {
		if (urlRewritePattern == null || urlRewriteFormat == null)
			return u;
		Matcher m = urlRewritePattern.matcher(u);
		if (!m.matches())
			return u;
		String result = String.format(urlRewriteFormat, m.group(1));
		return result;
	}

}
