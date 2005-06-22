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
 * A range of sequence numbers (inclusive).
 *
 * @author David Hovemeyer
 */
public class SequenceInterval implements Comparable<SequenceInterval> {
	private final long begin, end;
	
	/**
	 * Constructor.
	 * 
	 * @param begin beginning of sequence range (inclusive)
	 * @param end   end of sequence range (inclusive)
	 */
	public SequenceInterval(long begin, long end) {
		if (begin > end)
			throw new IllegalArgumentException("begin > end in sequence interval");
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
	 * Return whether or not the interval contains given sequence number.
	 * 
	 * @param sequence the sequence number
	 * @return true if the sequence number is contained in this interval, false if not
	 */
	public boolean contains(long sequence) {
		return sequence >= begin && sequence <= end;
	}

	/**
	 * Decode a String representing a SequenceInterval.
	 * The String should be in the form <i>begin</i>-<i>end</i>,
	 * where <i>begin</i> and <i>end</i> both specify <code>long</code>
	 * values representing the beginning and end of the interval.
	 * 
	 * @param token the interval
	 * @return the SequenceInterval
	 */
	public static SequenceInterval decode(String token) throws InvalidSequenceIntervalException {
		try {
			int dash = token.indexOf('-');
			if (dash < 0) {
				long moment = Long.parseLong(token);
				return new SequenceInterval(moment, moment);
			} else {
				String begin = token.substring(0, dash);
				String end = token.substring(dash+1);
				return new SequenceInterval(Long.parseLong(begin), Long.parseLong(end));
			}
		} catch (NumberFormatException e) {
			throw new InvalidSequenceIntervalException("Invalid interval: " + token, e);
		} catch (IllegalArgumentException e) {
			throw new InvalidSequenceIntervalException("Invalid interval: " + token, e);
		}
	}

	/**
	 * Encode a SequenceInterval as a String.
	 * The encoded String will be in the form <i>begin</i>-<i>end</i>,
	 * where <i>begin</i> and <i>end</i> both specify <code>long</code>
	 * values representing the beginning and end of the interval.
	 * 
	 * @param interval the SequenceInterval
	 * @return the encoded String
	 */
	public static String encode(SequenceInterval interval) {
		return interval.toString();
	}

	public int compareTo(SequenceInterval other) {
		long diff = this.begin - other.begin;
		if (diff != 0L)
			return signOf(diff);
		diff = this.end - other.end;
		return signOf(diff);
	}

	private static int signOf(long diff) {
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
	 * @param a a SequenceInterval
	 * @param b another SequenceInterval
	 * @return true if the intervals overlap, false if they don't
	 */
	public static boolean overlap(SequenceInterval a, SequenceInterval b) {
		if (a.begin > b.begin) {
			SequenceInterval tmp = a;
			a = b;
			b = tmp;
		}
		
		return b.begin <= a.end;
	}
	
	/**
	 * Merge overlapping intervals.
	 * 
	 * @param a a SequenceInterval
	 * @param b another SequenceInterval
	 * @return a new merged SequenceInterval
	 */
	public static SequenceInterval merge(SequenceInterval a, SequenceInterval b) {
		return new SequenceInterval(Math.min(a.begin,b.begin), Math.max(a.end,b.end));
	}
	
	// @Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(begin);
		buf.append('-');
		buf.append(end);
		return buf.toString();
	}
	
	// @Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass())
			return false;
		SequenceInterval other = (SequenceInterval) obj;
		return this.begin == other.begin
			&& this.end == other.end;
	}
	
	// @Override
	public int hashCode() {
		return (int)(begin * 1009 + end);
	}
	
}
