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

/**
 * A range of timestamps (inclusive).
 *
 * @author David Hovemeyer
 */
public class TimestampInterval implements Comparable<TimestampInterval> {
	private final long begin, end;
	
	/**
	 * Constructor.
	 * 
	 * @param begin beginning of timestamp range (inclusive)
	 * @param end   end of timestamp range (inclusive)
	 */
	public TimestampInterval(long begin, long end) {
		if (begin > end)
			throw new IllegalArgumentException("begin > end in timestamp interval");
		this.begin = begin;
		this.end = end;
	}

	/**
	 * Get the beginning of the range (inclusive).
	 * 
	 * @return the beginning of the range (inclusive)
	 */
	public long getBegin() {
		return begin;
	}
	

	/**
	 * Get the end of the range (inclusive).
	 * 
	 * @return the end of the range (inclusive)
	 */
	public long getEnd() {
		return end;
	}
	
	/**
	 * Return whether or not the interval contains given timestamp.
	 * 
	 * @param timestamp the timestamp
	 * @return true if the timestamp is contained in this interval, false if not
	 */
	public boolean contains(long timestamp) {
		return timestamp >= begin && timestamp <= end;
	}

	/**
	 * Decode a String representing a TimestampInterval.
	 * The String should be in the form <i>begin</i>-<i>end</i>,
	 * where <i>begin</i> and <i>end</i> both specify <code>long</code>
	 * values representing the beginning and end of the interval.
	 * 
	 * @param token the interval
	 * @return the TimestampInterval
	 * @throws NumberFormatException if the interval is invalid
	 * @throws IllegalArgumentException if the interval is invalid
	 */
	public static TimestampInterval decode(String token) {
		int dash = token.indexOf('-');
		if (dash < 0) {
			long moment = Long.parseLong(token);
			return new TimestampInterval(moment, moment);
		} else {
			String begin = token.substring(0, dash);
			String end = token.substring(dash+1);
			return new TimestampInterval(Long.parseLong(begin), Long.parseLong(end));
		}
	}

	/**
	 * Encode a TimestampInterval as a String.
	 * The encoded String will be in the form <i>begin</i>-<i>end</i>,
	 * where <i>begin</i> and <i>end</i> both specify <code>long</code>
	 * values representing the beginning and end of the interval.
	 * 
	 * @param interval the TimestampInterval
	 * @return the encoded String
	 */
	public static String encode(TimestampInterval interval) {
		return interval.toString();
	}

	public int compareTo(TimestampInterval other) {
		long diff = this.begin - other.begin;
		if (diff > 0L)
			return 1;
		else if (diff < 0L)
			return -1;
		else
			return 0;
	}
	
	/**
	 * Do given intervals overlap?
	 * 
	 * @param a a TimestampInterval
	 * @param b another TimestampInterval
	 * @return true if the intervals overlap, false if they don't
	 */
	public static boolean overlap(TimestampInterval a, TimestampInterval b) {
		if (a.begin > b.begin) {
			TimestampInterval tmp = a;
			a = b;
			b = tmp;
		}
		
		return b.begin <= a.end;
	}
	
	/**
	 * Merge overlapping intervals.
	 * 
	 * @param a a TimestampInterval
	 * @param b another TimestampInterval
	 * @return a new merged TimestampInterval
	 */
	public static TimestampInterval merge(TimestampInterval a, TimestampInterval b) {
		return new TimestampInterval(Math.min(a.begin,b.begin), Math.max(a.end,b.end));
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(begin);
		buf.append('-');
		buf.append(end);
		return buf.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass())
			return false;
		TimestampInterval other = (TimestampInterval) obj;
		return this.begin == other.begin
			&& this.end == other.end;
	}
	
	@Override
	public int hashCode() {
		return (int)(begin * 1009 + end);
	}
	
}
