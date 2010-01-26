/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.cloud.username;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.util.LaunchBrowser;
import edu.umd.cs.findbugs.util.Util;

/**
 * @author pugh
 */
public class AppEngineNameLookup implements NameLookup {
	

	public static final String LOCAL_APPENGINE = "appengine.local";
	
    public static final String APPENGINE_LOCALHOST_PROPERTY_NAME = "appengine.host.local";
    public static final String APPENGINE_LOCALHOST_DEFAULT = "http://localhost:8080";
    public static final String APPENGINE_HOST_PROPERTY_NAME = "appengine.host";
	
	private long sessionId;
	private String username;
	private String host;
	
	public boolean initialize(CloudPlugin plugin, BugCollection bugCollection) {
			try {
			boolean localAppengine = plugin.getProperties().getBoolean(LOCAL_APPENGINE);
			String h;
			if (localAppengine)
				h =plugin.getProperties().getProperty(APPENGINE_LOCALHOST_PROPERTY_NAME, APPENGINE_LOCALHOST_DEFAULT);
			else 
				h = plugin.getProperties().getProperty(APPENGINE_HOST_PROPERTY_NAME);
			host = h;
			SecureRandom r = new SecureRandom();
			long id = r.nextLong();
			URL u = new URL(host + "/browser-auth/" + id);
			LaunchBrowser.showDocument(u);
			URL response = new URL(host + "/check-auth/" + id);
			for (int i = 0; i < 60; i++) {
				HttpURLConnection connection = (HttpURLConnection) response.openConnection();
	
				int responseCode = connection.getResponseCode();
				if (responseCode == 200) {
					BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					String status = in.readLine();
					sessionId = Long.parseLong(in.readLine());
					username = in.readLine();
					Util.closeSilently(in);
					if ("OK".equals(status))
						return true;
	
				}
				connection.disconnect();
				Thread.sleep(1000);
			}
			return false;

		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public long getSessionId() {
    	return sessionId;
    }

	public String getUsername() {
    	return username;
    }
	
	public String getHost() {
		return host;
	}

}
