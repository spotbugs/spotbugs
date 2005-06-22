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
 * A collection of SequenceIntervals.
 * 
 * @author David Hovemeyer
 */
public class SequenceIntervalCollection {
	private List<SequenceInterval> intervalList;
	
	/**
	 * Constructor.
	 * Creates empty collection.
	 */
	public SequenceIntervalCollection() {
		this.intervalList = new ArrayList<SequenceInterval>();
	}
	
	/**
	 * Get an Iterator over the SequenceIntervals in the collection.
	 * Returns them in ascending order.
	 * 
	 * @return an Iterator over the intervals in ascending order
	 */
	public Iterator<SequenceInterval> intervalIterator() {
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
	public SequenceInterval get(int index) {
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
	public void add(SequenceInterval interval) {
		intervalList.add(interval);
		sort();
		simplify();
	}
	
	/**
	 * Return whether or not the interval collection contains the
	 * given sequence number.
	 * 
	 * @param sequence the sequence number
	 * @return true if the sequence number is contained in one of the intervals
	 *         in the collection, false if not
	 */
	public boolean contains(long sequence) {
		return findInterval(sequence) >= 0;
	}
	
	/**
	 * Find the index of the interval containing given sequence number.
	 * 
	 * @param sequence the sequence number
	 * @return the index of the interval containing the sequence number,
	 *         or -1 if no interval contains the sequence number
	 */
	public int findInterval(long sequence) {
		int min = 0;
		int max = intervalList.size();
		
		while (min <= max) {
			int mid = min + (max-min)/2;
			SequenceInterval interval = intervalList.get(mid);
			if (interval.contains(sequence)) {
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
	 * Decode SequenceIntervalCollection from String.
	 * The String should be a list of encoded TimeStampIntervals separated
	 * by commas.
	 * 
	 * @param s the encoded String
	 * @return the decoded SequenceIntervalCollection
	 * @throws InvalidSequenceIntervalException if any specified interval is invalid
	 */
	public static SequenceIntervalCollection decode(String s)
			throws InvalidSequenceIntervalException {
		SequenceIntervalCollection result = new SequenceIntervalCollection();
		StringTokenizer t = new StringTokenizer(s, ",");
		while (t.hasMoreTokens()) {
			String token = t.nextToken();
			result.intervalList.add(SequenceInterval.decode(token));
		}
		result.sort();
		result.simplify();
		return result;
	}
	
	/**
	 * Encode a SequenceIntervalCollection as a String.
	 * The String will be a list of encoded TimeStampIntervals separated
	 * by commas.
	 * 
	 * @param the SequenceIntervalCollection
	 * @return the encoded String representing the collection
	 */
	public static String encode(SequenceIntervalCollection collection) {
		return collection.toString();
	}
	
	/**
	 * Combine two SequenceIntervalCollections into a single collection.
	 * Overlapping intervals are coalesced.
	 * 
	 * @param a a SequenceIntervalCollection
	 * @param b another SequenceIntervalCollection
	 * @return the combined SequenceIntervalCollection
	 */
	public static SequenceIntervalCollection merge(
			SequenceIntervalCollection a, SequenceIntervalCollection b) {
		SequenceIntervalCollection result = new SequenceIntervalCollection();
		
		// SequenceIntervals are immutable, so we can just copy them directly
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
		for (Iterator<SequenceInterval> i = intervalIterator(); i.hasNext();) {
			if (buf.length() > 0)
				buf.append(",");
			buf.append(SequenceInterval.encode(i.next()));
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
			SequenceInterval a = intervalList.get(cur), b = intervalList.get(next);
			if (SequenceInterval.overlap(a,b)) {
				intervalList.set(cur, SequenceInterval.merge(a,b));
			} else {
				intervalList.set(++cur, b);
			}
			++next;
		}
		
		// Remove excess elements
		intervalList.subList(cur+1, origSize).clear();
	}
}
