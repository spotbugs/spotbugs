/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A collection of TimestampIntervals.
 * 
 * @author David Hovemeyer
 */
public class TimestampIntervalCollection {
	private List<TimestampInterval> intervalList;
	
	/**
	 * Constructor.
	 * Creates empty collection.
	 */
	public TimestampIntervalCollection() {
		this.intervalList = new ArrayList<TimestampInterval>();
	}
	
	/**
	 * Get an Iterator over the TimestampIntervals in the collection.
	 * Returns them in ascending order.
	 * 
	 * @return an Iterator over the intervals in ascending order
	 */
	public Iterator<TimestampInterval> intervalIterator() {
		return intervalList.iterator();
	}
	
	/**
	 * Return the number of intervals in the collection.
	 * 
	 * @return number of intervals in the collection
	 */
	public int size() {
		return intervalList.size();
	}
	
	/**
	 * Get interval specified by given index.
	 * 
	 * @param index the index of the interval to get
	 * @return the interval specified by the index
	 */
	public TimestampInterval get(int index) {
		return intervalList.get(index);
	}
	
	/**
	 * Remove the interval at the given index.
	 * 
	 * @param index the index of the interval to remove
	 */
	public void remove(int index) {
		intervalList.remove(index);
	}
	
	/**
	 * Add an interval to the collection.
	 * The collection is sorted, and if the new interval
	 * overlaps any existing intervals, they are coalesced.
	 * 
	 * @param interval the interval to add
	 */
	public void add(TimestampInterval interval) {
		intervalList.add(interval);
		sort();
		simplify();
	}
	
	/**
	 * Return whether or not the interval collection contains the
	 * given timestamp.
	 * 
	 * @param timestamp the timestamp
	 * @return true if the timestamp is contained in one of the intervals
	 *         in the collection, false if not
	 */
	public boolean contains(long timestamp) {
		return findInterval(timestamp) >= 0;
	}
	
	/**
	 * Find the index of the interval containing given timestamp.
	 * 
	 * @param timestamp the timestamp
	 * @return the index of the interval containing the timestamp,
	 *         or -1 if no interval contains the timestamp
	 */
	public int findInterval(long timestamp) {
		int min = 0;
		int max = intervalList.size();
		
		while (min <= max) {
			int mid = min + (max-min)/2;
			TimestampInterval interval = intervalList.get(mid);
			if (interval.contains(timestamp)) {
				return mid;
			}
			if (mid < interval.getBegin()) {
				max = mid - 1;
			} else {
				min = mid + 1;
			}
		}
		
		return -1;
	}

	/**
	 * Decode TimestampIntervalCollection from String.
	 * The String should be a list of encoded TimeStampIntervals separated
	 * by commas.
	 * 
	 * @param s the encoded String
	 * @return the decoded TimestampIntervalCollection
	 * @throws InvalidTimestampIntervalException if any specified interval is invalid
	 */
	public static TimestampIntervalCollection decode(String s)
			throws InvalidTimestampIntervalException {
		TimestampIntervalCollection result = new TimestampIntervalCollection();
		StringTokenizer t = new StringTokenizer(s, ",");
		while (t.hasMoreTokens()) {
			String token = t.nextToken();
			result.intervalList.add(TimestampInterval.decode(token));
		}
		result.sort();
		result.simplify();
		return result;
	}
	
	/**
	 * Encode a TimestampIntervalCollection as a String.
	 * The String will be a list of encoded TimeStampIntervals separated
	 * by commas.
	 * 
	 * @param the TimestampIntervalCollection
	 * @return the encoded String representing the collection
	 */
	public static String encode(TimestampIntervalCollection collection) {
		return collection.toString();
	}
	
	/**
	 * Combine two TimestampIntervalCollections into a single collection.
	 * Overlapping intervals are coalesced.
	 * 
	 * @param a a TimestampIntervalCollection
	 * @param b another TimestampIntervalCollection
	 * @return the combined TimestampIntervalCollection
	 */
	public static TimestampIntervalCollection merge(
			TimestampIntervalCollection a, TimestampIntervalCollection b) {
		TimestampIntervalCollection result = new TimestampIntervalCollection();
		
		// TimestampIntervals are immutable, so we can just copy them directly
		// into the result object.
		result.intervalList.addAll(a.intervalList);
		result.intervalList.addAll(b.intervalList);
		
		// Take care of ordering and overlap.
		result.sort();
		result.simplify();
		
		return result;
	}
	
	// @Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (Iterator<TimestampInterval> i = intervalIterator(); i.hasNext();) {
			if (buf.length() > 0)
				buf.append(",");
			buf.append(TimestampInterval.encode(i.next()));
		}
		return buf.toString().intern();// Save memory for identical interval collections
	}
	
	/**
	 * Sort intervals.
	 */
	private void sort() {
		Collections.sort(this.intervalList);
	}

	/**
	 * Coalesce overlapping intervals.
	 */
	private void simplify() {
		int origSize = intervalList.size();
		if (origSize == 0)
			return;
		
		// Merge adjacent intervals if they overlap.
		int cur = 0, next = 1;
		while (next < origSize) {
			TimestampInterval a = intervalList.get(cur), b = intervalList.get(next);
			if (TimestampInterval.overlap(a,b)) {
				intervalList.set(cur, TimestampInterval.merge(a,b));
			} else {
				intervalList.set(++cur, b);
			}
			++next;
		}
		
		// Remove excess elements
		intervalList.subList(cur+1, origSize).clear();
	}
}
