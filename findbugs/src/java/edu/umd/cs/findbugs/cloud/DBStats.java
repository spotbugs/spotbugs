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

package edu.umd.cs.findbugs.cloud;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.umd.cs.findbugs.PluginLoader;
import edu.umd.cs.findbugs.util.MergeMap;
import edu.umd.cs.findbugs.util.Multiset;



/**
 * @author pwilliam
 */
public class DBStats {
	
	static Timestamp bucketByHour(Timestamp t) {
		Timestamp result = new Timestamp(t.getTime());
		result.setSeconds(0);
		result.setMinutes(0);
		result.setNanos(0);
		return result;
	}
	
	
	static class TimeSeries<K, V extends Comparable<? super V>> implements Comparable<TimeSeries<K,V>>{
		final K k;
		final int keyHash;
		final V v;


        public TimeSeries(K k, V v) {
	        this.k = k;
	        this.keyHash = System.identityHashCode(k);
	        this.v = v;
        }
        
       public String toString() {
    	   return v + " " + k;
       }

		public int compareTo(TimeSeries<K, V> o) {
			if (o == this) return 0;
			int result = v.compareTo(o.v);
			if (result != 0)
				return result;
			if (keyHash < o.keyHash) 
				return -1;
			return 1;
        }
	}
	
	public static void main(String args[]) throws Exception {
		 Map<String,String> officeLocation = new HashMap<String,String>();
		
		URL u = PluginLoader.getCoreResource("offices.properties");
		if (u != null) {
			BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));
			while(true) {
				String s = in.readLine();
				if (s == null) break;
				if (s.trim().length() == 0)
					continue;
				int x = s.indexOf(':');
				
				if (x == -1)
					continue;
				String office = s.substring(0,x);
				for(String person : s.substring(x+1).split(" ")) 
					officeLocation.put(person, office);

				
			}
			in.close();
		}
		
		
		
		DBCloud cloud = new DBCloud(null);
		cloud.initialize();
		Connection c = cloud.getConnection();
		
		
		PreparedStatement ps = c.prepareStatement("SELECT who, entryPoint, dataSource, fbVersion, jvmLoadTime, findbugsLoadTime, analysisLoadTime, initialSyncTime, numIssues, startTime, commonPrefix"
					+ " FROM findbugs_invocation");
		
		MergeMap.MinMap <String, Timestamp> firstUse = new MergeMap.MinMap<String,Timestamp>();
		MergeMap.MinMap <String, Timestamp> reviewers = new MergeMap.MinMap<String,Timestamp>();
		MergeMap.MinMap <String, Timestamp> uniqueReviews = new MergeMap.MinMap<String,Timestamp>();
		
		HashSet<String> participants = new HashSet<String>();
		Multiset<String> participantsPerOffice = new Multiset<String>(new TreeMap<String,Integer>());
		ResultSet rs = ps.executeQuery();
		int invocationCount = 0;
		long invocationTotal = 0;
		long loadTotal = 0;
		while (rs.next()) {
			int col = 1;
			String who = rs.getString(col++);
			String entryPoint = rs.getString(col++);
			String dataSource = rs.getString(col++);
			String fbVersion = rs.getString(col++);
			int jvmLoad = rs.getInt(col++);
			int fbLoad = rs.getInt(col++);
			int analysisLoad = rs.getInt(col++);
			int dbSync = rs.getInt(col++);
			int numIssues = rs.getInt(col++);
			Timestamp when = rs.getTimestamp(col++);
			invocationCount++;
			invocationTotal += jvmLoad + fbLoad + analysisLoad + dbSync;
			loadTotal +=  fbLoad + analysisLoad + dbSync;
			firstUse.put(who, when);
			if (participants.add(who)) {
				String office = officeLocation.get(who);
				if (office == null) office = "unknown";
				participantsPerOffice.add(office);
			}
				
		}
		rs.close();
		ps.close();
		
		ps = c.prepareStatement("SELECT id, issueId, who, designation, comment, time FROM findbugs_evaluation ORDER BY time DESC");
		rs = ps.executeQuery();
		
		Multiset<String> issueReviewedBy =  new Multiset<String>();
		
		Multiset<String> allIssues = new Multiset<String>();
		Multiset<String> scariestIssues = new Multiset<String>();
		Multiset<String> scaryIssues = new Multiset<String>();
		Multiset<String> troublingIssues = new Multiset<String>();
		
		HashSet<String> issueReviews = new HashSet<String>();
		while (rs.next()) {
			int col = 1;
			int id = rs.getInt(col++);
			int issueId = rs.getInt(col++);
			String who = rs.getString(col++);
			String designation = rs.getString(col++);
			String comment = rs.getString(col++);
			Timestamp when = rs.getTimestamp(col++);
			reviewers.put(who, when);
			String issueReviewer = who+"-" + issueId;
			if (issueReviews.add(issueReviewer)) {
				uniqueReviews.put(issueReviewer, when);
				allIssues.add(designation);
				issueReviewedBy.add(who);
			}
				
		}
		rs.close();
		
		c.close();
		
		System.out.printf("%6d invocations\n", invocationCount);
		System.out.printf("%6d invocations time\n", invocationTotal/invocationCount);
		System.out.printf("%6d load time\n", loadTotal/invocationCount);
		System.out.println();
		
		printTimeSeries("Unique users", firstUse);
		printTimeSeries("Unique reviewers", reviewers);
		printTimeSeries("Total reviews", uniqueReviews);	
		
		System.out.println("Designations");
		for(Map.Entry<String, Integer> e : allIssues.entrySet())
			System.out.printf("%s,%d\n", e.getKey(), e.getValue());
		
		System.out.println();
		System.out.println("Participants by office");
		for(Map.Entry<String, Integer> e : participantsPerOffice.entrySet())
			System.out.printf("%s,%d\n", e.getKey(), e.getValue());
		System.out.println();
		PrintWriter w = new PrintWriter(System.out);
		
		DBCloud.printLeaderBoard(w, issueReviewedBy, 6, "", true, "num issues reviewed");
		w.flush();
	
	}

	private static void printTimeSeries(String title, MergeMap.MinMap<String, Timestamp> firstUse) {
		System.out.println(title);
	    TreeSet<TimeSeries<String, Timestamp>> series = new TreeSet<TimeSeries<String, Timestamp>>();
		for(Map.Entry<String, Timestamp> e : firstUse.entrySet()) {
			series.add(new TimeSeries<String,Timestamp>(e.getKey(), e.getValue()));
		}
		
		Multiset<Timestamp> counter = new Multiset<Timestamp>(new TreeMap<Timestamp, Integer>());
		for(TimeSeries<String, Timestamp> t : series) {
			counter.add(bucketByHour(t.v));
		}
		int total = 0;
		SimpleDateFormat format = new SimpleDateFormat("h a EEE");
		for(Map.Entry<Timestamp, Integer> e : counter.entrySet()) {
			total += e.getValue();
			System.out.printf("%4d, %s\n", total, format.format(e.getKey()));
		}
		System.out.println();
		
    }

}
