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

package edu.umd.cs.findbugs.cloud.db;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.cloud.NameLookup;
import edu.umd.cs.findbugs.util.LaunchBrowser;
import edu.umd.cs.findbugs.util.Util;

/**
 * @author pugh
 */
public class AppEngineNameLookup implements NameLookup {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.findbugs.cloud.NameLookup#getUserName(edu.umd.cs.findbugs.
	 * BugCollection)
	 */
	public String getUserName(BugCollection bugCollection) {
		try {
			SecureRandom r = new SecureRandom();
			long id = r.nextLong();
			URL u = new URL("http://theflybush.appspot.com/browser-auth/" + id);
			LaunchBrowser.showDocument(u);
			URL response = new URL("http://theflybush.appspot.com/check-auth/" + id);
			for (int i = 0; i < 60; i++) {
				System.out.println("Connecting");
				HttpURLConnection connection = (HttpURLConnection) response.openConnection();

				int responseCode = connection.getResponseCode();
				System.out.println(responseCode);
				if (responseCode == 200) {
					BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					String status = in.readLine();
					System.out.println(status);
					String idString = in.readLine();
					String name = in.readLine();
					Util.closeSilently(in);
					if (status.equals("OK"))
						return name;

				}
				connection.disconnect();
				Thread.sleep(1000);
			}

			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

}
