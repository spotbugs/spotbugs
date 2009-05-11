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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

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
		DBCloud cloud = new DBCloud(null);
		cloud.initialize();
		Connection c = cloud.getConnection();
		
		
		PreparedStatement queryInvocations = c.prepareStatement("SELECT who, entryPoint, dataSource, fbVersion, jvmLoadTime, findbugsLoadTime, analysisLoadTime, initialSyncTime, numIssues, startTime, commonPrefix"
					+ " FROM findbugs_invocation");
		
		MergeMap.MinMap <String, Timestamp> firstUse = new MergeMap.MinMap<String,Timestamp>();
		
		ResultSet rs = queryInvocations.executeQuery();
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
			firstUse.put(who, when);
		}
		rs.close();
		queryInvocations.close();
		
		c.close();
		TreeSet<TimeSeries<String, Timestamp>> series = new 	TreeSet<TimeSeries<String, Timestamp>>();
		for(Map.Entry<String, Timestamp> e : firstUse.entrySet()) {
			series.add(new TimeSeries(e.getKey(), e.getValue()));
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
			
	
	
	}

}
